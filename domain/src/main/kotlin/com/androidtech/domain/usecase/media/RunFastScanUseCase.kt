package com.androidtech.domain.usecase.media

import com.androidtech.domain.model.media.FastScanSummary
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.repository.MediaStoreRepository
import com.androidtech.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * Fast Scan: a raw MediaStore metadata pass with no bitmap decoding, no hashing — just counting.
 * Emits a running [FastScanSummary] as batches stream in from [MediaStoreRepository.fetchAllMediaFast],
 * so the UI can render a first result in well under a second and watch totals climb rather than
 * blocking on the full library.
 */
class RunFastScanUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository,
    private val storageRepository: StorageRepository,
) {

    operator fun invoke(): Flow<FastScanSummary> {
        val storageStats = storageRepository.getDeviceStorageStats()
        val initialSummary = FastScanSummary.EMPTY.copy(
            deviceTotalBytes = storageStats.totalBytes,
            deviceUsedBytes = storageStats.usedBytes,
            deviceFreeBytes = storageStats.freeBytes
        )

        return mediaStoreRepository.fetchAllMediaFast()
            .scan(initialSummary) { accumulated, batch -> accumulated.plusBatch(batch) }
            .flowOn(Dispatchers.Default) // summing is cheap but keep it off the caller's dispatcher
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
        const val LARGE_VIDEO_THRESHOLD_BYTES = 50L * 1024 * 1024 // 50MB
        const val SCREENSHOT_HINT = "screenshot"
    }
}