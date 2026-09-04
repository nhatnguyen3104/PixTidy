package com.androidtech.domain.usecase.media

import com.androidtech.domain.engine.ImageAnalyzerEngine
import com.androidtech.domain.engine.RecommendationEngine
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProgress
import com.androidtech.domain.model.media.ScanState
import com.androidtech.domain.model.media.SimilarGroup
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
import javax.inject.Inject

/**
 * Smart Scan: the full six-stage pipeline —
 *  a) fast metadata filter, b) exact duplicates, c) similar photos, d) quality analysis,
 *  e) screenshot classification, f) recommendation scoring — emitted as a single [ScanState]
 * stream so the UI needs only one collector.
 *
 * Memory/backpressure strategy: candidates are bucketed then processed with a bounded [Semaphore]
 * so at most [MAX_CONCURRENT_DECODES] bitmaps are ever decoded at once, regardless of library
 * size — this is the main OOM guard, since bitmap decode (not hashing) is what actually spikes
 * native memory.
 */
class RunSmartScanUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository,
    private val imageAnalyzerEngine: ImageAnalyzerEngine,
    private val recommendationEngine: RecommendationEngine
) {

    operator fun invoke(): Flow<ScanState> = callbackFlow {
        trySend(scanning(0, 1, "Preparing scan\u2026"))

        val job = launch {
            try {
                runPipeline { state -> trySend(state) }
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                trySend(ScanState.Error(t))
            } finally {
                close()
            }
        }

        awaitClose { job.cancel() }
    }.flowOn(Dispatchers.Default)

    private suspend fun runPipeline(emit: (ScanState) -> Unit) = coroutineScope {
        // Stage a: candidate fetch + fast metadata filter
        emit(scanning(0, 1, "Reading media library\u2026"))
        val allCandidates = mediaStoreRepository.fetchCandidatesForSmartScan()
        val candidates = allCandidates.filter { it.hasValidDimensions }
        val total = candidates.size.coerceAtLeast(1)
        ensureActive()

        val images = candidates.filter { it.isImage }
        val videos = candidates.filter { it.isVideo }

        // Stage e: screenshot classification (cheap, metadata-only — do it first, exclude from
        // dedup/similarity/quality passes so a folder of 200 screenshots doesn't dominate them)
        val screenshots = images.filter { isLikelyScreenshot(it) }
        val screenshotIds = screenshots.mapTo(HashSet()) { it.id }
        val nonScreenshotImages = images.filterNot { it.id in screenshotIds }

        // Stage e (video variant): large videos, independent of the image pipeline below
        val largeVideos = videos.filter { it.size >= LARGE_VIDEO_THRESHOLD_BYTES }

        // Stage b: exact duplicates — bucket by size first (near-zero cost), only hash within a
        // bucket that has >= 2 members, since a unique size can never collide byte-for-byte.
        emit(scanning(0, total, "Looking for exact duplicates\u2026"))
        val sizeBuckets = nonScreenshotImages.groupBy { it.size }.filterValues { it.size >= 2 }
        val duplicateGroups = findExactDuplicates(sizeBuckets.values.flatten()) { processed ->
            emit(scanning(processed, total, "Looking for exact duplicates\u2026"))
        }
        val exactDuplicateIds = duplicateGroups.flatten().mapTo(HashSet()) { it.id }
        val nonDuplicateImages = nonScreenshotImages.filterNot { it.id in exactDuplicateIds }

        // Stage c: similar (not identical) photos — perceptual hash + Hamming clustering,
        // bucketed by rounded aspect ratio first to avoid an O(n^2) compare across the whole library.
        emit(scanning(0, total, "Finding similar photos\u2026"))
        val similarGroups = findSimilarGroups(nonDuplicateImages) { processed ->
            emit(scanning(processed, total, "Finding similar photos\u2026"))
        }
        val similarGroupedIds = similarGroups.flatMap { it.items }.mapTo(HashSet()) { it.id }

        // Stage d: quality analysis on whatever is left ungrouped (a duplicate/similar item's
        // quality doesn't change the recommendation — the dup/similarity signal already dominates)
        emit(scanning(0, total, "Checking photo quality\u2026"))
        val qualityCandidates = nonDuplicateImages.filterNot { it.id in similarGroupedIds }
        val blurryItems = findBlurryItems(qualityCandidates) { processed ->
            emit(scanning(processed, total, "Checking photo quality\u2026"))
        }

        // Stage f: recommendation scoring
        emit(scanning(total, total, "Generating recommendations\u2026"))
        val insights = recommendationEngine.generateInsights(
            duplicates = duplicateGroups,
            similarGroups = similarGroups,
            blurry = blurryItems,
            screenshots = screenshots,
            largeVideos = largeVideos
        )
        val totalReclaimable = insights.sumOf { it.reclaimableBytes }

        emit(ScanState.Success(insights = insights, totalReclaimableBytes = totalReclaimable))
    }

    /** Groups items by exact content hash within each pre-bucketed (equal-size) group. */
    private suspend fun findExactDuplicates(
        sizeBucketedCandidates: List<MediaItem>,
        onProgress: suspend (processed: Int) -> Unit
    ): List<List<MediaItem>> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT_DECODES)
        val processedCount = java.util.concurrent.atomic.AtomicInteger(0)

        val hashed = sizeBucketedCandidates.map { item ->
            async {
                semaphore.withPermit {
                    currentCoroutineContext().ensureActive()
                    val hash = runCatching { imageAnalyzerEngine.calculateContentHash(item) }.getOrNull()
                    onProgress(processedCount.incrementAndGet())
                    hash?.let { item to it }
                }
            }
        }.awaitAll().filterNotNull()

        hashed.groupBy({ it.second }, { it.first })
            .values
            .filter { it.size >= 2 }
    }

    /** Buckets by rounded aspect ratio, then clusters within each bucket via Hamming distance. */
    private suspend fun findSimilarGroups(
        candidates: List<MediaItem>,
        onProgress: suspend (processed: Int) -> Unit
    ): List<SimilarGroup> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT_DECODES)
        val processedCount = java.util.concurrent.atomic.AtomicInteger(0)

        val hashed = candidates.map { item ->
            async {
                semaphore.withPermit {
                    currentCoroutineContext().ensureActive()
                    val hash = runCatching { imageAnalyzerEngine.calculatePhash(item) }.getOrNull()
                    onProgress(processedCount.incrementAndGet())
                    hash?.let { item to it }
                }
            }
        }.awaitAll().filterNotNull()

        val aspectBuckets = hashed.groupBy { (item, _) -> aspectBucketKey(item) }

        val groups = mutableListOf<SimilarGroup>()
        for (bucket in aspectBuckets.values) {
            groups += clusterByHammingDistance(bucket)
        }
        groups
    }

    /** Simple greedy single-link clustering: fine at bucket scale (dozens, not thousands). */
    private fun clusterByHammingDistance(bucket: List<Pair<MediaItem, String>>): List<SimilarGroup> {
        val unclustered = bucket.toMutableList()
        val result = mutableListOf<SimilarGroup>()

        while (unclustered.isNotEmpty()) {
            val seed = unclustered.removeAt(0)
            val cluster = mutableListOf(seed)

            val iterator = unclustered.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                val distance = PerceptualHashUtils.hammingDistance(seed.second, candidate.second)
                if (distance <= SIMILARITY_HAMMING_THRESHOLD) {
                    cluster += candidate
                    iterator.remove()
                }
            }

            if (cluster.size >= 2) {
                val items = cluster.map { it.first }
                val best = items.maxBy { it.width.toLong() * it.height.toLong() }
                val worstDistance = cluster.maxOf {
                    PerceptualHashUtils.hammingDistance(seed.second, it.second)
                }
                val savings = items.filterNot { it.id == best.id }.sumOf { it.size }
                result += SimilarGroup(
                    groupId = UUID.randomUUID().toString(),
                    items = items,
                    bestCandidateId = best.id,
                    similarityPercentage = PerceptualHashUtils.similarityPercentage(worstDistance),
                    potentialSavingsBytes = savings
                )
            }
        }
        return result
    }

    private suspend fun findBlurryItems(
        candidates: List<MediaItem>,
        onProgress: suspend (processed: Int) -> Unit
    ): List<MediaItem> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT_DECODES)
        val processedCount = java.util.concurrent.atomic.AtomicInteger(0)

        candidates.map { item ->
            async {
                semaphore.withPermit {
                    currentCoroutineContext().ensureActive()
                    val quality = runCatching { imageAnalyzerEngine.evaluateQuality(item) }.getOrNull()
                    onProgress(processedCount.incrementAndGet())
                    if (quality?.isBlurry == true) item else null
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun isLikelyScreenshot(item: MediaItem): Boolean {
        val nameOrPathHit = item.path.lowercase().contains(SCREENSHOT_HINT) ||
                item.name.lowercase().contains(SCREENSHOT_HINT)
        return nameOrPathHit
    }

    private fun aspectBucketKey(item: MediaItem): Int {
        if (item.height == 0) return 0
        val ratio = item.width.toFloat() / item.height.toFloat()
        return (ratio * 10).toInt() // buckets of 0.1 aspect-ratio granularity
    }

    private fun scanning(processed: Int, total: Int, message: String) =
        ScanState.Scanning(ScanProgress.of(ScanLayer.SMART_SCAN, processed, total, message))

    private companion object {
        const val MAX_CONCURRENT_DECODES = 4
        const val LARGE_VIDEO_THRESHOLD_BYTES = 100L * 1024 * 1024
        const val SIMILARITY_HAMMING_THRESHOLD = 10 // out of 64 bits
        const val SCREENSHOT_HINT = "screenshot"
    }
}
