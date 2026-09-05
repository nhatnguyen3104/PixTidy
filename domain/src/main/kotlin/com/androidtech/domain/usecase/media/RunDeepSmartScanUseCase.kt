package com.androidtech.domain.usecase.media

import com.androidtech.domain.engine.DeepAnalysisEngine
import com.androidtech.domain.engine.ImageAnalyzerEngine
import com.androidtech.domain.engine.RecommendationEngine
import com.androidtech.domain.model.media.FrameSample
import com.androidtech.domain.model.media.InsightType
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProgress
import com.androidtech.domain.model.media.ScanProcess
import com.androidtech.domain.model.media.SmartInsight
import com.androidtech.domain.repository.MediaStoreRepository
import com.androidtech.domain.utils.PerceptualHashUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Deep Smart Scan: everything in Smart Scan, plus the expensive analyses users opt into
 * explicitly — deep duplicate *video* detection (via sampled-frame perceptual hashing, since
 * videos can't be exact-hashed cheaply the way still images can when re-encoded), and OCR/document
 * detection on the images Smart Scan already found to not be duplicates or blurry junk (no point
 * running OCR on something already flagged for deletion).
 *
 * Runs on [Dispatchers.Default] throughout — this is explicitly the "safe in background" tier the
 * spec calls for, so it never touches Main and is fully cooperative-cancellation-friendly via
 * [ensureActive] checks between every item.
 */
class RunDeepSmartScanUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository,
    private val imageAnalyzerEngine: ImageAnalyzerEngine,
    private val deepAnalysisEngine: DeepAnalysisEngine,
    private val recommendationEngine: RecommendationEngine,
    private val runSmartScanUseCase: RunSmartScanUseCase
) {

    operator fun invoke(): Flow<ScanProcess> = callbackFlow {
        val job = launch(Dispatchers.Default) {
            try {
                runDeepPipeline { state -> trySend(state) }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                trySend(ScanProcess.Error(t))
            } finally {
                close()
            }
        }
        awaitClose { job.cancel() }
    }.flowOn(Dispatchers.Default)

    private suspend fun runDeepPipeline(emit: (ScanProcess) -> Unit) = coroutineScope {
        // Reuse the entire Smart Scan pipeline for its insights (duplicates/similar/blurry/
        // screenshots/large videos) rather than re-implementing it — Deep Smart Scan only adds
        // the two stages below on top.
        var smartScanInsights: List<SmartInsight> = emptyList()
        var smartScanReclaimable = 0L

        runSmartScanUseCase().collect { state ->
            when (state) {
                is ScanProcess.Scanning -> {
                    // Map Smart Scan (0-100%) to Deep Scan (0-70%)
                    val deepPct = state.progress.progressPercentage * 0.7f
                    emit(deepScanning(
                        state.progress.processedItems,
                        state.progress.totalItems,
                        state.progress.currentStepMessage,
                        deepPct
                    ))
                }
                is ScanProcess.Success -> {
                    smartScanInsights = state.insights
                    smartScanReclaimable = state.totalReclaimableBytes
                }
                is ScanProcess.Error -> throw state.throwable
                ScanProcess.Idle -> Unit
            }
        }

        val alreadyFlaggedIds = smartScanInsights.flatMap { it.items }.mapTo(HashSet()) { it.id }

        val allCandidates = mediaStoreRepository.fetchCandidatesForSmartScan()
            .filter { it.hasValidDimensions }
        val videos = allCandidates.filter { it.isVideo }
        val imagesNotAlreadyFlagged = allCandidates.filter { it.isImage && it.id !in alreadyFlaggedIds }
        
        val videoTotal = videos.size.coerceAtLeast(1)
        val imageTotal = imagesNotAlreadyFlagged.size.coerceAtLeast(1)

        // Stage: deep duplicate video detection via sampled-frame hashing
        emit(deepScanning(0, videoTotal, "Comparing videos frame-by-frame\u2026", 70f))
        val deepDuplicateVideoInsight = findDuplicateVideos(videos) { processed ->
            val pct = 70f + (processed.toFloat() / videoTotal * 15f)
            emit(deepScanning(processed, videoTotal, "Comparing videos frame-by-frame\u2026", pct))
        }

        // Stage: OCR / document detection on remaining images
        emit(deepScanning(0, imageTotal, "Scanning for documents & receipts\u2026", 85f))
        val documentInsight = findDocumentImages(imagesNotAlreadyFlagged) { processed ->
            val pct = 85f + (processed.toFloat() / imageTotal * 15f)
            emit(deepScanning(processed, imageTotal, "Scanning for documents & receipts\u2026", pct))
        }

        val deepInsights = listOfNotNull(deepDuplicateVideoInsight, documentInsight)
        val combinedInsights = smartScanInsights + deepInsights
        val combinedReclaimable = smartScanReclaimable + deepInsights.sumOf { it.reclaimableBytes }

        emit(ScanProcess.Success(insights = combinedInsights, totalReclaimableBytes = combinedReclaimable))
    }

    private suspend fun findDuplicateVideos(
        videos: List<MediaItem>,
        onProgress: suspend (Int) -> Unit
    ): SmartInsight? = coroutineScope {
        if (videos.size < 2) return@coroutineScope null

        val semaphore = Semaphore(MAX_CONCURRENT_VIDEO_DECODES)
        val processedCount = AtomicInteger(0)

        val sampled = videos.map { item ->
            async {
                semaphore.withPermit {
                    currentCoroutineContext().ensureActive()
                    val frames = runCatching {
                        deepAnalysisEngine.sampleKeyFrames(item)
                    }.getOrDefault(emptyList())
                    onProgress(processedCount.incrementAndGet())
                    item to frames
                }
            }
        }.awaitAll().filter { it.second.isNotEmpty() }

        val duplicateGroups = clusterVideosByFrameSimilarity(sampled)
        if (duplicateGroups.isEmpty()) return@coroutineScope null

        val allItems = duplicateGroups.flatten()
        val reclaimable = duplicateGroups.sumOf { group ->
            val keep = group.maxBy { it.size }
            group.filterNot { it.id == keep.id }.sumOf { it.size }
        }

        SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Duplicate videos",
            description = "${allItems.size} videos across ${duplicateGroups.size} group(s) look like the same recording.",
            type = InsightType.SIMILAR_PHOTOS, // videos share the perceptual-similarity concept; no dedicated video type in the given enum
            reclaimableBytes = reclaimable,
            affectedItemsCount = allItems.size,
            items = allItems,
            confidenceScore = 0.8f,
            explanation = "Sampled frames from each video and compared their visual fingerprints; " +
                    "these videos matched closely enough across the clip to likely be duplicates or re-exports."
        )
    }

    /** Two videos are considered duplicates if a majority of their sampled frames match closely. */
    private fun clusterVideosByFrameSimilarity(
        sampled: List<Pair<MediaItem, List<FrameSample>>>
    ): List<List<MediaItem>> {
        val unclustered = sampled.toMutableList()
        val groups = mutableListOf<List<MediaItem>>()

        while (unclustered.isNotEmpty()) {
            val seed = unclustered.removeAt(0)
            val cluster = mutableListOf(seed)

            val iterator = unclustered.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (framesMatch(seed.second, candidate.second)) {
                    cluster += candidate
                    iterator.remove()
                }
            }

            if (cluster.size >= 2) groups += cluster.map { it.first }
        }
        return groups
    }

    private fun framesMatch(a: List<FrameSample>, b: List<FrameSample>): Boolean {
        val pairCount = minOf(a.size, b.size)
        if (pairCount == 0) return false
        var matches = 0
        for (i in 0 until pairCount) {
            val distance = PerceptualHashUtils.hammingDistance(a[i].phash, b[i].phash)
            if (distance <= VIDEO_FRAME_HAMMING_THRESHOLD) matches++
        }
        return matches.toFloat() / pairCount.toFloat() >= VIDEO_FRAME_MATCH_RATIO
    }

    private suspend fun findDocumentImages(
        images: List<MediaItem>,
        onProgress: suspend (Int) -> Unit
    ): SmartInsight? = coroutineScope {
        if (images.isEmpty()) return@coroutineScope null

        val semaphore = Semaphore(MAX_CONCURRENT_OCR)
        val processedCount = AtomicInteger(0)

        val documents = images.map { item ->
            async {
                semaphore.withPermit {
                    currentCoroutineContext().ensureActive()
                    val result = runCatching { deepAnalysisEngine.analyzeDocument(item) }.getOrNull()
                    onProgress(processedCount.incrementAndGet())
                    if (result?.isLikelyDocument == true) item to result else null
                }
            }
        }.awaitAll().filterNotNull()

        if (documents.isEmpty()) return@coroutineScope null

        val items = documents.map { it.first }
        val avgConfidence = documents.map { it.second.confidence }.average().toFloat()

        SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Documents & receipts",
            description = "${items.size} photo(s) look like scanned text — receipts, whiteboards, or screenshots of documents.",
            type = InsightType.DEEP_OCR_DOCUMENTS,
            reclaimableBytes = items.sumOf { it.size },
            affectedItemsCount = items.size,
            items = items,
            confidenceScore = avgConfidence,
            explanation = "On-device text recognition found dense text regions typical of documents " +
                    "rather than regular photos. Review before deleting — some may be worth keeping or exporting to a notes app."
        )
    }

    private fun deepScanning(processed: Int, total: Int, message: String, percentage: Float? = null) =
        ScanProcess.Scanning(
            if (percentage != null) {
                ScanProgress.fixed(ScanLayer.DEEP_SMART_SCAN, processed, total, message, percentage)
            } else {
                ScanProgress.of(ScanLayer.DEEP_SMART_SCAN, processed, total, message)
            }
        )

    private companion object {
        const val MAX_CONCURRENT_VIDEO_DECODES = 2 // MediaMetadataRetriever is heavier than BitmapFactory
        const val MAX_CONCURRENT_OCR = 3
        const val VIDEO_FRAME_HAMMING_THRESHOLD = 10
        const val VIDEO_FRAME_MATCH_RATIO = 0.6f
    }
}