package com.androidtech.domain.usecase.media

import com.androidtech.domain.engine.ImageAnalyzerEngine
import com.androidtech.domain.engine.RecommendationEngine
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.QualityScore
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProgress
import com.androidtech.domain.model.media.ScanProcess
import com.androidtech.domain.model.media.SimilarGroup
import com.androidtech.domain.repository.MediaAnalysisCacheRepository
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
 * Smart Scan: the full six-stage pipeline —
 *  a) fast metadata filter, b) exact duplicates, c) similar photos, d) quality analysis,
 *  e) screenshot classification, f) recommendation scoring — emitted as a single [ScanProcess]
 * stream so the UI needs only one collector.
 *
 * Memory/backpressure strategy: candidates are bucketed then processed with a bounded [Semaphore]
 * so at most [MAX_CONCURRENT_DECODES] bitmaps are ever decoded at once, regardless of library
 * size — this is the main OOM guard, since bitmap decode (not hashing) is what actually spikes
 * native memory.
 *
 * Incremental caching: pHash + quality are resolved together per item via [resolveAnalyses],
 * which checks [MediaAnalysisCacheRepository] (keyed on mediaId + dateModified) before ever
 * touching [ImageAnalyzerEngine]. This is a deliberate change from computing them in two
 * independent stages: previously, quality analysis skipped any image already grouped as similar,
 * to save decode work on a cold run. With caching, both fields are resolved together for the full
 * non-exact-duplicate candidate set so each item's cache row is written exactly once. That costs a
 * little more decode time on a device's very first (cold-cache) scan, but every scan after that —
 * for any unchanged file — is served entirely from cache, which is the much larger win.
 */
class RunSmartScanUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository,
    private val imageAnalyzerEngine: ImageAnalyzerEngine,
    private val mediaAnalysisCacheRepository: MediaAnalysisCacheRepository,
    private val recommendationEngine: RecommendationEngine
) {

    operator fun invoke(): Flow<ScanProcess> = callbackFlow {
        trySend(scanning(0, 1, "Preparing scan\u2026"))

        val job = launch {
            try {
                runPipeline { state -> trySend(state) }
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

    private suspend fun runPipeline(emit: (ScanProcess) -> Unit) = coroutineScope {
        // Stage a: candidate fetch + fast metadata filter
        emit(scanning(0, 1, "Reading media library\u2026"))
        val allCandidates = mediaStoreRepository.fetchCandidatesForSmartScan()
        val candidates = allCandidates.filter { it.hasValidDimensions }
        val total = candidates.size.coerceAtLeast(1)
        ensureActive()

        // Best-effort housekeeping: drop cache rows for media no longer on device. Not awaited —
        // this must never block the scan; a rare miss against a row Room hasn't dropped yet is
        // harmless, it just means one extra fresh analysis next time that item is seen.
        launch {
            runCatching { mediaAnalysisCacheRepository.pruneOrphaned(candidates.map { it.id }) }
        }

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
        // pHash and quality are resolved together here (cache-aware — see class KDoc) so each
        // item's cache row is written once, covering both stage c and stage d's needs.
        emit(scanning(0, total, "Finding similar photos\u2026"))
        val analyses = resolveAnalyses(nonDuplicateImages) { processed ->
            emit(scanning(processed, total, "Finding similar photos\u2026"))
        }
        val similarGroups = findSimilarGroups(nonDuplicateImages, analyses)
        val similarGroupedIds = similarGroups.flatMap { it.items }.mapTo(HashSet()) { it.id }

        // Stage d: quality analysis on whatever is left ungrouped (a duplicate/similar item's
        // quality doesn't change the recommendation — the dup/similarity signal already dominates).
        // No engine calls here — [analyses] already has everything from stage c above.
        emit(scanning(total, total, "Checking photo quality\u2026"))
        val blurryItems = nonDuplicateImages.filter { item ->
            item.id !in similarGroupedIds && analyses[item.id]?.isBlurry == true
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

        emit(ScanProcess.Success(insights = insights, totalReclaimableBytes = totalReclaimable))
    }

    /** Groups items by exact content hash within each pre-bucketed (equal-size) group. */
    private suspend fun findExactDuplicates(
        sizeBucketedCandidates: List<MediaItem>,
        onProgress: suspend (processed: Int) -> Unit
    ): List<List<MediaItem>> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT_DECODES)
        val processedCount = AtomicInteger(0)

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

    /** Cache-aware per-item analysis: pHash + quality resolved together, one cache row per item. */
    private data class ResolvedAnalysis(val phash: String?, val isBlurry: Boolean)

    private suspend fun resolveAnalyses(
        candidates: List<MediaItem>,
        onProgress: suspend (processed: Int) -> Unit
    ): Map<Long, ResolvedAnalysis> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT_DECODES)
        val processedCount = AtomicInteger(0)

        candidates.map { item ->
            async {
                semaphore.withPermit {
                    currentCoroutineContext().ensureActive()
                    val analysis = resolveSingleAnalysis(item)
                    onProgress(processedCount.incrementAndGet())
                    item.id to analysis
                }
            }
        }.awaitAll().toMap()
    }

    private suspend fun resolveSingleAnalysis(item: MediaItem): ResolvedAnalysis {
        val cached = runCatching {
            mediaAnalysisCacheRepository.getValidCache(item.id, item.dateModified)
        }.getOrNull()

        if (cached != null) {
            return ResolvedAnalysis(phash = cached.phash, isBlurry = cached.blurScore < BLURRY_SCORE_THRESHOLD)
        }

        // Cache MISS (no row, or the file's dateModified has changed since it was last analyzed):
        // fall through to the real engine and persist the fresh result.
        val phash = runCatching { imageAnalyzerEngine.calculatePhash(item) }.getOrNull()
        val quality = runCatching { imageAnalyzerEngine.evaluateQuality(item) }.getOrNull() ?: QualityScore.UNKNOWN

        runCatching {
            mediaAnalysisCacheRepository.saveAnalysis(
                mediaId = item.id,
                dateModified = item.dateModified,
                phash = phash,
                blurScore = quality.score,
                // This method only ever runs on nonScreenshotImages (screenshots are filtered out
                // upstream by the cheap heuristic before resolveAnalyses is called), so the cached
                // flag is always false here — accurate for every row this pipeline writes.
                isScreenshot = false
            )
        }

        return ResolvedAnalysis(phash = phash, isBlurry = quality.isBlurry)
    }

    /** Buckets by rounded aspect ratio, then clusters within each bucket via Hamming distance. */
    private fun findSimilarGroups(
        candidates: List<MediaItem>,
        analyses: Map<Long, ResolvedAnalysis>
    ): List<SimilarGroup> {
        val hashed = candidates.mapNotNull { item ->
            analyses[item.id]?.phash?.let { phash -> item to phash }
        }

        val aspectBuckets = hashed.groupBy { (item, _) -> aspectBucketKey(item) }

        val groups = mutableListOf<SimilarGroup>()
        for (bucket in aspectBuckets.values) {
            groups += clusterByHammingDistance(bucket)
        }
        return groups
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
        ScanProcess.Scanning(ScanProgress.of(ScanLayer.SMART_SCAN, processed, total, message))

    private companion object {
        const val MAX_CONCURRENT_DECODES = 4
        const val LARGE_VIDEO_THRESHOLD_BYTES = 100L * 1024 * 1024
        const val SIMILARITY_HAMMING_THRESHOLD = 10 // out of 64 bits
        const val SCREENSHOT_HINT = "screenshot"
        // A cached blurScore is QualityScore.score (0..100), not the raw Laplacian variance;
        // this threshold is calibrated against BlurDetectorImpl's own variance->score mapping.
        const val BLURRY_SCORE_THRESHOLD = 20
    }
}

