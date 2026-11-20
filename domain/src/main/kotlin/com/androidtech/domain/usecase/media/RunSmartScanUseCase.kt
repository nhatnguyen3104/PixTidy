package com.androidtech.domain.usecase.media

import com.androidtech.domain.engine.ImageAnalyzerEngine
import com.androidtech.domain.engine.RecommendationEngine
import com.androidtech.domain.model.media.DuplicateGroup
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.QualityScore
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProgress
import com.androidtech.domain.model.media.ScanProcess
import com.androidtech.domain.model.media.ScanSummary
import com.androidtech.domain.model.media.SimilarGroup
import com.androidtech.domain.repository.MediaAnalysisCacheRepository
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
import java.util.concurrent.TimeUnit
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
    private val recommendationEngine: RecommendationEngine,
    private val systemStorageRepository: SystemStorageRepository
) {

    operator fun invoke(): Flow<ScanProcess> = callbackFlow {
        trySend(smartScanning("Preparing scan\u2026", 5f))

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
        emit(smartScanning("Reading media library\u2026", 15f))
        val allCandidates = mediaStoreRepository.fetchCandidatesForSmartScan()
        val candidates = allCandidates.filter { it.hasValidDimensions }
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

        // Stage e (video variant): relevant videos for categorization (screen recordings, old & long, massive >= 100MB)
        val now = System.currentTimeMillis()
        val largeVideos = videos.filter { item ->
            val pathLower = item.path.lowercase()
            val nameLower = item.name.lowercase()
            val isScreen = pathLower.contains("screen") || nameLower.contains("screen") ||
                    pathLower.contains("record") || nameLower.contains("record")
            val isOldAndLong = (now - item.dateAdded >= TimeUnit.DAYS.toMillis(30)) && ((item.duration ?: 0L) >= TimeUnit.MINUTES.toMillis(2))
            val isMassive = item.size >= LARGE_VIDEO_THRESHOLD_BYTES
            isScreen || isOldAndLong || isMassive
        }

        val globalTotal = allCandidates.size

        // Stage b: exact duplicates
        val baseProcessedB = globalTotal - nonScreenshotImages.size
        val duplicateGroups = imageAnalyzerEngine.findExactDuplicates(nonScreenshotImages) { processed, total ->
            val globalProcessed = baseProcessedB + processed
            val pct = 35f + (processed.toFloat() / total.coerceAtLeast(1).toFloat() * 30f)
            emit(smartScanningWithProgress("Looking for exact duplicates\u2026", globalProcessed, globalTotal, pct))
        }
        val exactDuplicateIds = duplicateGroups.flatMap(DuplicateGroup::allItems).mapTo(HashSet()) { it.id }
        val nonDuplicateImages = nonScreenshotImages.filterNot { it.id in exactDuplicateIds }

        // Stage c: similar (not identical) photos
        val baseProcessedC = globalTotal - nonDuplicateImages.size
        val similarGroups = imageAnalyzerEngine.findSimilarGroups(nonDuplicateImages) { processed, total ->
            val globalProcessed = baseProcessedC + processed
            val pct = 65f + (processed.toFloat() / total.coerceAtLeast(1).toFloat() * 20f)
            emit(smartScanningWithProgress("Finding similar photos\u2026", globalProcessed, globalTotal, pct))
        }
        val similarGroupedIds = similarGroups.flatMap { it.items }.mapTo(HashSet()) { it.id }

        // Stage d: quality analysis on whatever is left ungrouped
        val qualityCandidates = nonDuplicateImages.filterNot { it.id in similarGroupedIds }
        val baseProcessedD = globalTotal - qualityCandidates.size
        val blurryItems = imageAnalyzerEngine.findBlurryItems(qualityCandidates) { processed, total ->
            val globalProcessed = baseProcessedD + processed
            val pct = 85f + (processed.toFloat() / total.coerceAtLeast(1).toFloat() * 10f)
            emit(smartScanningWithProgress("Checking photo quality\u2026", globalProcessed, globalTotal, pct))
        }

        // Stage f: recommendation scoring
        emit(smartScanningWithProgress("Generating recommendations\u2026", globalTotal, globalTotal, 95f))
        val insights = recommendationEngine.generateInsights(
            duplicates = duplicateGroups,
            similarGroups = similarGroups,
            blurry = blurryItems,
            screenshots = screenshots,
            largeVideos = largeVideos
        )

        val storageSummary = systemStorageRepository.fetchDeviceStorage()
        val totalMediaSize = allCandidates.sumOf { it.size }

        val summary = ScanSummary(
            lastScanLevel = ScanLayer.SMART,
            newItemsCount = 0, // Not tracked in this use case yet
            totalMediaCount = allCandidates.size,
            totalStorageBytes = totalMediaSize,
            screenshotCount = screenshots.size,
            screenshotBytes = screenshots.sumOf { it.size },
            largeVideoCount = largeVideos.size,
            largeVideoBytes = largeVideos.sumOf { it.size },
            blurryCount = blurryItems.size,
            blurrySizeBytes = blurryItems.sumOf { it.size },
            photosSizeBytes = images.sumOf { it.size },
            videosSizeBytes = videos.sumOf { it.size },
            duplicatesSizeBytes = duplicateGroups.sumOf { it.totalSizeDifferenceBytes },
            similaritySizeBytes = similarGroups.sumOf { it.potentialSavingsBytes },
            storageSummary = storageSummary.copy(mediaUsedBytes = totalMediaSize)
        )

        val totalReclaimable = insights.sumOf { it.reclaimableBytes }

        emit(ScanProcess.Success(summary = summary, insights = insights, totalReclaimableBytes = totalReclaimable))
    }

    private fun isLikelyScreenshot(item: MediaItem): Boolean {
        val nameOrPathHit = item.path.lowercase().contains(SCREENSHOT_HINT) ||
                item.name.lowercase().contains(SCREENSHOT_HINT)
        return nameOrPathHit
    }

    private fun smartScanning(message: String, percentage: Float) =
        ScanProcess.Scanning(ScanProgress.fixed(ScanLayer.SMART, 0, 100, message, percentage))

    private fun smartScanningWithProgress(message: String, processed: Int, total: Int, percentage: Float) =
        ScanProcess.Scanning(ScanProgress.fixed(ScanLayer.SMART, processed, total, message, percentage))

    private companion object {
        const val LARGE_VIDEO_THRESHOLD_BYTES = 100L * 1024 * 1024
        const val SCREENSHOT_HINT = "screenshot"
    }
}


