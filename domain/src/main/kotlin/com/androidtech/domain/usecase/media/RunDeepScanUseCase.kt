package com.androidtech.domain.usecase.media

import com.androidtech.domain.engine.DeepAnalysisEngine
import com.androidtech.domain.engine.RecommendationEngine
import com.androidtech.domain.model.media.FrameSample
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProgress
import com.androidtech.domain.model.media.ScanProcess
import com.androidtech.domain.model.media.ScanSummary
import com.androidtech.domain.model.media.SmartInsight
import com.androidtech.domain.model.media.SmartInsightType
import com.androidtech.domain.repository.MediaStoreRepository
import com.androidtech.domain.repository.SystemStorageRepository
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
import kotlin.collections.sumOf

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
class RunDeepScanUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository,
    private val deepAnalysisEngine: DeepAnalysisEngine,
    private val recommendationEngine: RecommendationEngine,
    private val runSmartScanUseCase: RunSmartScanUseCase,
    private val systemStorageRepository: SystemStorageRepository
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
                    val scaledProgress = state.progress.copy(
                        currentLayer = ScanLayer.DEEP,
                        progressPercentage = state.progress.progressPercentage * 0.5f
                    )
                    emit(ScanProcess.Scanning(scaledProgress))
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

        val candidates = mediaStoreRepository.fetchCandidatesForSmartScan()
            .filter { it.hasValidDimensions }
        val videos = candidates.filter { it.isVideo }
        val imagesNotAlreadyFlagged = candidates.filter { it.isImage && it.id !in alreadyFlaggedIds }
        val total = (videos.size + imagesNotAlreadyFlagged.size).coerceAtLeast(1)

        // Stage: deep duplicate video detection via sampled-frame hashing
        emit(deepScanning(0, total, "Comparing videos frame-by-frame\u2026"))
        val deepDuplicateVideoInsight = findDuplicateVideos(videos) { processed ->
            emit(deepScanning(processed, total, "Comparing videos frame-by-frame\u2026"))
        }

        // Stage: OCR / document detection on remaining images
        emit(deepScanning(videos.size, total, "Scanning for documents & receipts\u2026"))
        val documentInsight = findDocumentImages(imagesNotAlreadyFlagged) { processed ->
            emit(deepScanning(videos.size + processed, total, "Scanning for documents & receipts\u2026"))
        }

        val deepInsights = listOfNotNull(deepDuplicateVideoInsight, documentInsight)
        val combinedInsights = smartScanInsights + deepInsights
        val combinedReclaimable = smartScanReclaimable + deepInsights.sumOf { it.reclaimableBytes }

        val storageSummary = systemStorageRepository.fetchDeviceStorage()
        val totalMediaSize = candidates.sumOf { it.size }

        var screenshotCount = 0
        var screenshotBytes = 0L
        var largeVideoCount = 0
        var largeVideoBytes = 0L
        var blurryCount = 0
        var blurrySizeBytes = 0L
        var duplicatesBytes = 0L
        var similarityBytes = 0L
        var documentCount = 0
        var documentBytes = 0L

        combinedInsights.forEach { insight ->
            when (insight.type) {
                SmartInsightType.EXACT_DUPLICATES, SmartInsightType.DUPLICATE_VIDEOS -> {
                    duplicatesBytes += insight.reclaimableBytes
                }
                SmartInsightType.SIMILAR_PHOTOS -> {
                    similarityBytes += insight.reclaimableBytes
                }
                SmartInsightType.SCREENSHOTS -> {
                    screenshotCount = insight.affectedItemsCount
                    screenshotBytes = insight.reclaimableBytes
                }
                SmartInsightType.LARGE_VIDEOS -> {
                    largeVideoCount = insight.affectedItemsCount
                    largeVideoBytes = insight.reclaimableBytes
                }
                SmartInsightType.BLURRY_PHOTOS -> {
                    blurryCount = insight.affectedItemsCount
                    blurrySizeBytes = insight.reclaimableBytes
                }
                SmartInsightType.DEEP_OCR_DOCUMENTS -> {
                    documentCount = insight.affectedItemsCount
                    documentBytes = insight.reclaimableBytes
                }
                else -> {}
            }
        }

        val summary = ScanSummary(
            lastScanLevel = ScanLayer.DEEP,
            newItemsCount = 0, // Not tracked in this use case yet
            totalMediaCount = candidates.size,
            totalStorageBytes = totalMediaSize,
            screenshotCount = screenshotCount,
            screenshotBytes = screenshotBytes,
            largeVideoCount = largeVideoCount,
            largeVideoBytes = largeVideoBytes,
            blurryCount = blurryCount,
            blurrySizeBytes = blurrySizeBytes,
            photosSizeBytes = 0L,
            videosSizeBytes = 0L,
            duplicatesSizeBytes = duplicatesBytes,
            similaritySizeBytes = similarityBytes,
            documentCount = documentCount,
            documentBytes = documentBytes,
            storageSummary = storageSummary.copy(mediaUsedBytes = totalMediaSize)
        )

        emit(ScanProcess.Success(insights = combinedInsights,summary = summary , totalReclaimableBytes = combinedReclaimable))
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

        val allItems = duplicateGroups.flatMap { group ->
            val groupId = UUID.randomUUID().toString()
            val keep = group.maxBy { it.size }
            group.map { item ->
                item.copy(
                    groupId = groupId,
                    isBestKeep = item.id == keep.id
                )
            }
        }
        val reclaimable = duplicateGroups.sumOf { group ->
            val keep = group.maxBy { it.size }
            group.filterNot { it.id == keep.id }.sumOf { it.size }
        }

        SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Duplicate videos",
            description = "${allItems.size} videos across ${duplicateGroups.size} group(s) look like the same recording.",
            type = SmartInsightType.DUPLICATE_VIDEOS,
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
            type = SmartInsightType.DEEP_OCR_DOCUMENTS,
            reclaimableBytes = items.sumOf { it.size },
            affectedItemsCount = items.size,
            items = items,
            confidenceScore = avgConfidence,
            explanation = "On-device text recognition found dense text regions typical of documents " +
                    "rather than regular photos. Review before deleting — some may be worth keeping or exporting to a notes app."
        )
    }

    private fun deepScanning(processed: Int, total: Int, message: String): ScanProcess {
        val safeTotal = total.coerceAtLeast(1)
        val phasePercentage = (processed.toFloat() / safeTotal.toFloat()) * 50f
        return ScanProcess.Scanning(ScanProgress.fixed(ScanLayer.DEEP, processed, total, message, 50f + phasePercentage))
    }

    private companion object {
        const val MAX_CONCURRENT_VIDEO_DECODES = 2 // MediaMetadataRetriever is heavier than BitmapFactory
        const val MAX_CONCURRENT_OCR = 3
        const val VIDEO_FRAME_HAMMING_THRESHOLD = 10
        const val VIDEO_FRAME_MATCH_RATIO = 0.6f
    }
}