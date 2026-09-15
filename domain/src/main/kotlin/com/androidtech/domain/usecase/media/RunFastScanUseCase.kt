package com.androidtech.domain.usecase.media

import com.androidtech.domain.model.media.ScanSummary
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.StorageSummary
import com.androidtech.domain.repository.MediaAnalysisCacheRepository
import com.androidtech.domain.repository.MediaStoreRepository
import com.androidtech.domain.repository.ScanResultsRepository
import com.androidtech.domain.repository.SystemStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fast Scan: Performs a raw MediaStore metadata pass to synchronize the library state
 * and provide an instant storage snapshot.
 *
 * It serves as the "Sync" layer, updating Room with current file counts and identifying
 * newly added items that haven't been analyzed by Smart Scan yet.
 */
class RunFastScanUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository,
    private val systemStorageRepository: SystemStorageRepository,
    private val mediaAnalysisCacheRepository: MediaAnalysisCacheRepository,
    private val scanResultsRepository: ScanResultsRepository
) {

    operator fun invoke(): Flow<ScanSummary> = flow {
        coroutineScope {
            val storageSummaryDeferred = async { buildStorageSummary() }
            val allMediaIds = mutableListOf<Long>()
            var lastEmittedSummary: ScanSummary? = null

            mediaStoreRepository.fetchAllMediaFast()
                .scan(ScanSummary.EMPTY.copy(lastScanLevel = ScanLayer.FAST)) { accumulated, batch ->
                    allMediaIds.addAll(batch.map { it.id })
                    accumulated.plusBatch(batch)
                }
                .drop(1)
                .onEach { runningSummary ->
                    val summaryWithStorage = runningSummary.copy(storageSummary = storageSummaryDeferred.await())
                    lastEmittedSummary = summaryWithStorage
                    emit(summaryWithStorage)
                }
                .onCompletion {
                    // Sync: Prune orphaned cache entries
                    launch {
                        mediaAnalysisCacheRepository.pruneOrphaned(allMediaIds)
                    }
                    // Persist the final sync result to Room
                    lastEmittedSummary?.let { final ->
                        launch {
                            scanResultsRepository.saveResults(final, emptyList())
                        }
                    }
                }
                .collect {}
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun buildStorageSummary(): StorageSummary = coroutineScope {
        val deviceStorageDeferred = async { systemStorageRepository.fetchDeviceStorage() }
        val mediaSizeDeferred = async { mediaStoreRepository.fetchTotalMediaSizeBytes() }
        deviceStorageDeferred.await().copy(mediaUsedBytes = mediaSizeDeferred.await())
    }

    private fun ScanSummary.plusBatch(batch: List<MediaItem>): ScanSummary {
        var addedBytes = 0L
        var addedScreenshots = 0
        var addedScreenshotBytes = 0L
        var addedLargeVideos = 0
        var addedLargeVideoBytes = 0L
        var addedVideoBytes = 0L
        var addedPhotoBytes = 0L

        for (item in batch) {
            addedBytes += item.size
            if (isLikelyScreenshot(item)) {
                addedScreenshots++
                addedScreenshotBytes += item.size
            }
            if (item.isVideo) {
                addedVideoBytes += item.size
                if (item.size >= LARGE_VIDEO_THRESHOLD_BYTES) {
                    addedLargeVideos++
                    addedLargeVideoBytes += item.size
                }
            } else {
                addedPhotoBytes += item.size
            }
        }

        return copy(
            totalMediaCount = totalMediaCount + batch.size,
            totalStorageBytes = totalStorageBytes + addedBytes,
            screenshotCount = screenshotCount + addedScreenshots,
            screenshotBytes = screenshotBytes + addedScreenshotBytes,
            largeVideoCount = largeVideoCount + addedLargeVideos,
            largeVideoBytes = largeVideoBytes + addedLargeVideoBytes,
            photosSizeBytes = photosSizeBytes + addedPhotoBytes,
            videosSizeBytes = videosSizeBytes + addedVideoBytes
        )
    }

    private fun isLikelyScreenshot(item: MediaItem): Boolean {
        val needle = SCREENSHOT_HINT
        return item.path.lowercase().contains(needle) || item.name.lowercase().contains(needle)
    }

    private companion object {
        const val LARGE_VIDEO_THRESHOLD_BYTES = 100L * 1024 * 1024 // 100MB
        const val SCREENSHOT_HINT = "screenshot"
    }
}
