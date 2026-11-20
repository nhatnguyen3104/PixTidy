package com.androidtech.data.analyzer

import com.androidtech.data.logger.DataLogger
import com.androidtech.domain.engine.ImageAnalyzerEngine
import com.androidtech.domain.model.media.DuplicateGroup
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.QualityScore
import com.androidtech.domain.model.media.SimilarGroup
import com.androidtech.domain.repository.MediaAnalysisCacheRepository
import com.androidtech.domain.utils.PerceptualHashUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Composition root for the domain [ImageAnalyzerEngine] contract. The three low-level primitives
 * ([calculatePhash], [evaluateQuality], [calculateContentHash]) each delegate to their own small,
 * independently testable class; the three grouping methods ([findExactDuplicates],
 * [findSimilarGroups], [findBlurryItems]) are built on top of those primitives and own the
 * clustering algorithm and the [MediaAnalysisCacheRepository] incremental-caching logic — moved
 * here from the use case layer so the algorithm and its caching live in exactly one place.
 *
 * Memory/backpressure: every grouping method processes candidates through a bounded [Semaphore]
 * so at most [MAX_CONCURRENT_DECODES] bitmaps are ever decoded at once, regardless of library size.
 */
class ImageAnalyzerEngineImpl @Inject constructor(
    private val perceptualHashAnalyzer: FastPerceptualHashAnalyzer,
    private val blurDetector: BlurDetectorImpl,
    private val contentHasher: ContentHasher,
    private val mediaAnalysisCacheRepository: MediaAnalysisCacheRepository
) : ImageAnalyzerEngine {

    override suspend fun calculatePhash(item: MediaItem): String? =
        perceptualHashAnalyzer.computePhash(item)

    override suspend fun evaluateQuality(item: MediaItem): QualityScore =
        blurDetector.evaluate(item)

    override suspend fun calculateContentHash(item: MediaItem): String? =
        contentHasher.sha256(item)

    // ---- Exact duplicates -----------------------------------------------------------------------

    override suspend fun findExactDuplicates(
        candidates: List<MediaItem>,
        onProgress: ((processed: Int, total: Int) -> Unit)?
    ): List<DuplicateGroup> = coroutineScope {
        DataLogger.d("ExactDup: Candidates count = ${candidates.size}")
        val zeroSizeCount = candidates.count { it.size == 0L }
        if (zeroSizeCount > 0) {
            DataLogger.e("ExactDup WARNING: Found $zeroSizeCount items with size = 0!")
        }

        // Bucket by size first (near-zero cost): a unique size can never collide byte-for-byte, so
        // only hash within a bucket that already has >= 2 members. Content hash is not cached (the
        // cache schema only covers pHash/blurScore/isScreenshot) — full-file hashing is a one-pass
        // stream read, not a bitmap decode, so it's cheap enough to redo each scan.
        val sizeBucketed = candidates.groupBy { it.size }.filterValues { it.size >= 2 }.values.flatten()
        DataLogger.d("ExactDup: Items with potential size collisions = ${sizeBucketed.size}")

        val processedCount = AtomicInteger(candidates.size - sizeBucketed.size)
        onProgress?.invoke(processedCount.get(), candidates.size)

        val semaphore = Semaphore(MAX_CONCURRENT_DECODES)
        val hashed = sizeBucketed.map { item ->
            async {
                semaphore.withPermit {
                    currentCoroutineContext().ensureActive()
                    val hash = runCatching { calculateContentHash(item) }.getOrNull()
                    val processed = processedCount.incrementAndGet()
                    onProgress?.invoke(processed, candidates.size)
                    hash?.let { item to it }
                }
            }
        }.awaitAll().filterNotNull()

        hashed.groupBy({ it.second }, { it.first })
            .filterValues { it.size >= 2 }
            .map { (hash, items) -> toDuplicateGroup(hash, items) }
    }

    private fun toDuplicateGroup(hash: String, items: List<MediaItem>): DuplicateGroup {
        // "Original to keep": oldest creation date, highest resolution as a tie-breaker.
        val original = items.sortedWith(
            compareBy<MediaItem> { it.dateAdded }.thenByDescending { it.width.toLong() * it.height }
        ).first()
        val duplicates = items.filterNot { it.id == original.id }

        return DuplicateGroup(
            groupId = UUID.randomUUID().toString(),
            originalItem = original,
            duplicateItems = duplicates,
            totalSizeDifferenceBytes = duplicates.sumOf { it.size },
            hash = hash
        )
    }

    // ---- Similar (not identical) photos -----------------------------------------------------------

    override suspend fun findSimilarGroups(
        candidates: List<MediaItem>,
        onProgress: ((processed: Int, total: Int) -> Unit)?
    ): List<SimilarGroup> = coroutineScope {
        val analyses = resolveAnalyses(candidates, onProgress)

        val hashed = candidates.mapNotNull { item ->
            analyses[item.id]?.phash?.let { phash -> Triple(item, phash, analyses.getValue(item.id).qualityScore) }
        }

        DataLogger.d("ExactSim: Candidates=${candidates.size}, " + "ValidPHash=${hashed.size}")

        val aspectBuckets = hashed.groupBy { (item, _, _) -> aspectBucketKey(item) }
        DataLogger.d("ExactSim: Aspect bucket distribution = ${aspectBuckets.mapValues { it.value.size }}")
        aspectBuckets.values.flatMap { bucket -> clusterByHammingDistance(bucket) }
    }

    /** Simple greedy single-link clustering: fine at bucket scale (dozens, not thousands). */
    private fun clusterByHammingDistance(
        bucket: List<Triple<MediaItem, String, Int>>
    ): List<SimilarGroup> {
        val unclustered = bucket.toMutableList()
        val result = mutableListOf<SimilarGroup>()

        while (unclustered.isNotEmpty()) {
            val seed = unclustered.removeAt(0)
            val cluster = mutableListOf(seed)

            val queue = mutableListOf(seed)

            while (queue.isNotEmpty()) {
                val current = queue.removeAt(0)
                val iterator = unclustered.iterator()

                while (iterator.hasNext()) {
                    val candidate = iterator.next()
                    val distance =
                        PerceptualHashUtils.hammingDistance(current.second, candidate.second)

                    if (distance <= SIMILARITY_HAMMING_THRESHOLD) {
                        cluster.add(candidate)
                        queue.add(candidate)
                        iterator.remove()
                    }
                }
            }

            if (cluster.size >= 2) {
                result += buildSimilarGroup(seed, cluster)
            }
        }
        return result
    }

    private fun buildSimilarGroup(
        seed: Triple<MediaItem, String, Int>,
        cluster: List<Triple<MediaItem, String, Int>>
    ): SimilarGroup {
        val items = cluster.map { it.first }
        val maxResolution = items.maxOf { it.width.toLong() * it.height }.coerceAtLeast(1L)

        // "Best candidate": combine quality score and resolution, not resolution alone — each
        // normalized to 0..100 and weighted evenly.
        val best = cluster.maxBy { (item, _, qualityScore) ->
            val resolutionScore = (item.width.toLong() * item.height * 100 / maxResolution)
            qualityScore * QUALITY_WEIGHT + resolutionScore * RESOLUTION_WEIGHT
        }.first

        val worstDistance = cluster.maxOf { PerceptualHashUtils.hammingDistance(seed.second, it.second) }
        val savings = items.filterNot { it.id == best.id }.sumOf { it.size }

        return SimilarGroup(
            groupId = UUID.randomUUID().toString(),
            items = items,
            bestCandidateId = best.id,
            similarityPercentage = PerceptualHashUtils.similarityPercentage(worstDistance),
            potentialSavingsBytes = savings
        )
    }

    // ---- Blurry items (for whatever wasn't already grouped above) -------------------------------

    override suspend fun findBlurryItems(
        candidates: List<MediaItem>,
        onProgress: ((processed: Int, total: Int) -> Unit)?
    ): List<MediaItem> = coroutineScope {
        // Calling resolveAnalyses again here is intentionally cheap, not wasteful: any item that
        // was also part of the findSimilarGroups() candidate set for this same scan will already
        // be sitting in the in-memory cache tier (see MediaAnalysisCacheRepositoryImpl), so this is
        // typically all cache hits with zero further decode work.
        val analyses = resolveAnalyses(candidates, onProgress)
        candidates.filter { item -> analyses[item.id]?.isBlurry == true }
            .map { item ->
                val qualityScore = analyses[item.id]?.qualityScore ?: 50
                val blurLevel = (100 - qualityScore).coerceIn(0, 100)
                item.copy(blurScore = blurLevel)
            }
    }

    // ---- Shared cache-aware per-item resolution ---------------------------------------------------

    private data class ResolvedAnalysis(val phash: String?, val qualityScore: Int, val isBlurry: Boolean)

    private suspend fun resolveAnalyses(
        candidates: List<MediaItem>,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null
    ): Map<Long, ResolvedAnalysis> = coroutineScope {
        val semaphore = Semaphore(MAX_CONCURRENT_DECODES)
        val processedCount = AtomicInteger(0)

        if (candidates.isEmpty()) {
            onProgress?.invoke(0, 0)
        }

        candidates.map { item ->
            async {
                semaphore.withPermit {
                    currentCoroutineContext().ensureActive()
                    val analysis = resolveSingleAnalysis(item)
                    val processed = processedCount.incrementAndGet()
                    onProgress?.invoke(processed, candidates.size)
                    item.id to analysis
                }
            }
        }.awaitAll().toMap()
    }

    private suspend fun resolveSingleAnalysis(item: MediaItem): ResolvedAnalysis {
        val cached = runCatching {
            mediaAnalysisCacheRepository.getValidCache(item.id, item.dateModified)
        }.getOrNull()

        if (cached != null && cached.phash != null) {
            return ResolvedAnalysis(
                phash = cached.phash,
                qualityScore = cached.blurScore,
                isBlurry = cached.blurScore < BLURRY_SCORE_THRESHOLD
            )
        }

        if (cached != null) {
            DataLogger.d("ExactSim: Cache exists but pHash is NULL for id=${item.id}, re-calculating...")
        }

        // Cache MISS or incomplete cache (no row, or stored phash is null, or file changed):
        // fall through to the real engine primitives and persist the fresh result.
        val phash = runCatching { calculatePhash(item) }.getOrNull()
        if (phash == null) {
            DataLogger.w("ExactSim: pHash calculated as NULL for id=${item.id}, name=${item.name}, uri=${item.uri}")
        }
        val quality = runCatching { evaluateQuality(item) }.getOrNull() ?: QualityScore.UNKNOWN

        runCatching {
            mediaAnalysisCacheRepository.saveAnalysis(
                mediaId = item.id,
                dateModified = item.dateModified,
                phash = phash,
                blurScore = quality.score,
                // findExactDuplicates/findSimilarGroups/findBlurryItems only ever run on images
                // already filtered to non-screenshots upstream (RunSmartScanUseCase), so this is
                // accurate for every row this engine writes.
                isScreenshot = false
            )
        }

        return ResolvedAnalysis(phash = phash, qualityScore = quality.score, isBlurry = quality.isBlurry)
    }

    private fun aspectBucketKey(item: MediaItem): Int {
        if (item.height == 0) return 0
        val ratio = item.width.toFloat() / item.height.toFloat()
        //return (ratio * 10).toInt() // buckets of 0.1 aspect-ratio granularity
        return when {
            ratio > 1.1f -> 1
            ratio < 0.9f -> 2
            else         -> 3
        }
    }

    private companion object {
        const val MAX_CONCURRENT_DECODES = 4
        const val SIMILARITY_HAMMING_THRESHOLD = 10 // out of 64 bits (tightened to prevent false positives from color-similar images)
        const val QUALITY_WEIGHT = 1L
        const val RESOLUTION_WEIGHT = 1L
        // A cached/computed blurScore is QualityScore.score (0..100), not the raw Laplacian
        // variance; this threshold is calibrated against BlurDetectorImpl's variance->score mapping.
        const val BLURRY_SCORE_THRESHOLD = 20
    }
}