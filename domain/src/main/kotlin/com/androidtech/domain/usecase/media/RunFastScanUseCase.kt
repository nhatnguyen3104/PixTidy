package com.androidtech.domain.usecase.media

import com.androidtech.domain.model.media.FastScanSummary
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.StorageSummary
import com.androidtech.domain.repository.MediaStoreRepository
import com.androidtech.domain.repository.SystemStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.scan
import javax.inject.Inject

/**
 * Fast Scan: a raw MediaStore metadata pass with no bitmap decoding, no hashing — just counting —
 * combined with system-wide storage totals, so the very first screen the user sees after launch
 * has both "how much space do I have" and "what's on it" without waiting for Smart Scan.
 *
 * [StorageSummary] (total/used/free device bytes + total media bytes) is fetched once, up front,
 * concurrently with the very first MediaStore batch via [async] — both [SystemStorageRepository.fetchDeviceStorage]
 * (a StatFs/StorageStatsManager syscall) and [MediaStoreRepository.fetchTotalMediaSizeBytes] (a
 * single-column pass, not a full record materialization) are cheap enough that running them
 * alongside the first batch keeps the whole thing well under the 500ms target. It does not change
 * per batch afterward — unlike [FastScanSummary.totalStorageBytes], which is a running count of
 * *this scan's* progress, [FastScanSummary.storageSummary] reflects the device's actual current state.
 */
class RunFastScanUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository,
    private val systemStorageRepository: SystemStorageRepository
) {

    operator fun invoke(): Flow<FastScanSummary> = flow {
        coroutineScope {
            val storageSummaryDeferred = async { buildStorageSummary() }

            mediaStoreRepository.fetchAllMediaFast()
                .scan(FastScanSummary.EMPTY) { accumulated, batch -> accumulated.plusBatch(batch) }
                .drop(1) // drop the seed emitted by scan() before any real batch has arrived
                .collect { runningSummary ->
                    // Resolves immediately after the first await; every batch after the first
                    // sees an already-completed Deferred, so this costs nothing further.
                    emit(runningSummary.copy(storageSummary = storageSummaryDeferred.await()))
                }
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun buildStorageSummary(): StorageSummary = coroutineScope {
        val deviceStorageDeferred = async { systemStorageRepository.fetchDeviceStorage() }
        val mediaSizeDeferred = async { mediaStoreRepository.fetchTotalMediaSizeBytes() }
        deviceStorageDeferred.await().copy(mediaUsedBytes = mediaSizeDeferred.await())
    }

    private fun FastScanSummary.plusBatch(batch: List<MediaItem>): FastScanSummary {
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

    /** Cheap path/name heuristic — the real classifier (dimension-ratio aware) runs in Smart Scan. */
    private fun isLikelyScreenshot(item: MediaItem): Boolean {
        val needle = SCREENSHOT_HINT
        return item.path.lowercase().contains(needle) || item.name.lowercase().contains(needle)
    }

    private companion object {
        const val LARGE_VIDEO_THRESHOLD_BYTES = 100L * 1024 * 1024 // 100MB
        const val SCREENSHOT_HINT = "screenshot"
    }
}
