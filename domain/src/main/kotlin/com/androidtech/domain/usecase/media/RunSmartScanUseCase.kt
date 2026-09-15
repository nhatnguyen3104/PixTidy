package com.androidtech.domain.usecase.media

import com.androidtech.domain.engine.ImageAnalyzerEngine
import com.androidtech.domain.engine.RecommendationEngine
import com.androidtech.domain.model.media.DuplicateGroup
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
 * The concrete duplicate/similarity clustering algorithm — including the pHash/quality incremental
 * caching against [MediaAnalysisCacheRepository] — lives in [ImageAnalyzerEngine]'s implementation,
 * not here: this use case only orchestrates the stage order, tracks progress, and hands each
 * stage's leftovers to the next one so a photo already explained by a duplicate/similarity finding
 * isn't redundantly flagged as blurry too.
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

        // Stage b: exact duplicates
        emit(scanning(0, total, "Looking for exact duplicates\u2026"))
        val duplicateGroups = imageAnalyzerEngine.findExactDuplicates(nonScreenshotImages)
        val exactDuplicateIds = duplicateGroups.flatMap(DuplicateGroup::allItems).mapTo(HashSet()) { it.id }
        val nonDuplicateImages = nonScreenshotImages.filterNot { it.id in exactDuplicateIds }

        // Stage c: similar (not identical) photos
        emit(scanning(0, total, "Finding similar photos\u2026"))
        val similarGroups = imageAnalyzerEngine.findSimilarGroups(nonDuplicateImages)
        val similarGroupedIds = similarGroups.flatMap { it.items }.mapTo(HashSet()) { it.id }

        // Stage d: quality analysis on whatever is left ungrouped (a duplicate/similar item's
        // quality doesn't change the recommendation — the dup/similarity signal already dominates)
        emit(scanning(total, total, "Checking photo quality\u2026"))
        val qualityCandidates = nonDuplicateImages.filterNot { it.id in similarGroupedIds }
        val blurryItems = imageAnalyzerEngine.findBlurryItems(qualityCandidates)

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

    private fun isLikelyScreenshot(item: MediaItem): Boolean {
        val nameOrPathHit = item.path.lowercase().contains(SCREENSHOT_HINT) ||
                item.name.lowercase().contains(SCREENSHOT_HINT)
        return nameOrPathHit
    }

    private fun scanning(processed: Int, total: Int, message: String) =
        ScanProcess.Scanning(ScanProgress.of(ScanLayer.SMART, processed, total, message))

    private companion object {
        const val LARGE_VIDEO_THRESHOLD_BYTES = 100L * 1024 * 1024
        const val SCREENSHOT_HINT = "screenshot"
    }
}


