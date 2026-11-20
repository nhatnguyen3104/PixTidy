package com.androidtech.domain.usecase.media

import com.androidtech.domain.model.media.ScanSummary
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.isLargeVideo
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
import kotlinx.coroutines.flow.firstOrNull
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

    operator fun invoke(hasPermission: Boolean = true): Flow<ScanSummary> = flow {
        if (!hasPermission) {
            val storageSummary = buildStorageSummary(hasPermission = false)
            emit(ScanSummary.EMPTY.copy(lastScanLevel = ScanLayer.NONE, storageSummary = storageSummary))
            return@flow
        }
        coroutineScope {
            val storageSummaryDeferred = async { buildStorageSummary(hasPermission = true) }
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
                    // Persist the final sync result to Room without wiping deeper scan insights
                    lastEmittedSummary?.let { final ->
                        launch {
                            val currentSummary = scanResultsRepository.getLatestResults().firstOrNull()
                            val currentInsights = scanResultsRepository.getLatestInsights().firstOrNull() ?: emptyList()

                            val mergedSummary = if (currentSummary != null && (currentSummary.lastScanLevel == ScanLayer.SMART || currentSummary.lastScanLevel == ScanLayer.DEEP)) {
                                final.copy(
                                    lastScanLevel = currentSummary.lastScanLevel,
                                    duplicatesSizeBytes = currentSummary.duplicatesSizeBytes,
                                    similaritySizeBytes = currentSummary.similaritySizeBytes,
                                    screenshotCount = if (currentSummary.screenshotCount > 0) currentSummary.screenshotCount else final.screenshotCount,
                                    screenshotBytes = if (currentSummary.screenshotBytes > 0) currentSummary.screenshotBytes else final.screenshotBytes,
                                    largeVideoCount = if (currentSummary.largeVideoCount > 0) currentSummary.largeVideoCount else final.largeVideoCount,
                                    largeVideoBytes = if (currentSummary.largeVideoBytes > 0) currentSummary.largeVideoBytes else final.largeVideoBytes
                                )
                            } else {
                                final
                            }

                            val insightsToSave = if (currentSummary != null && (currentSummary.lastScanLevel == ScanLayer.SMART || currentSummary.lastScanLevel == ScanLayer.DEEP)) {
                                currentInsights
                            } else {
                                emptyList()
                            }

                            scanResultsRepository.saveResults(mergedSummary, insightsToSave)
                        }
                    }
                }
                .collect {}
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun buildStorageSummary(hasPermission: Boolean = true): StorageSummary = coroutineScope {
        val deviceStorageDeferred = async { systemStorageRepository.fetchDeviceStorage() }
        val mediaSizeDeferred = if (hasPermission) {
            async { mediaStoreRepository.fetchTotalMediaSizeBytes() }.await()
        } else {
            0L
        }
        deviceStorageDeferred.await().copy(mediaUsedBytes = mediaSizeDeferred)
    }

    private fun ScanSummary.plusBatch(batch: List<MediaItem>): ScanSummary {
        var addedBytes = 0L
        var addedLargeVideos = 0
        var addedLargeVideoBytes = 0L
        var addedVideoBytes = 0L
        var addedPhotoBytes = 0L

        for (item in batch) {
            addedBytes += item.size
            if (item.isVideo) {
                addedVideoBytes += item.size
                if (item.isLargeVideo) {
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
            screenshotCount = 0,
            screenshotBytes = 0L,
            /*largeVideoCount = largeVideoCount + addedLargeVideos,
            largeVideoBytes = largeVideoBytes + addedLargeVideoBytes,*/
            largeVideoCount = 0,
            largeVideoBytes = 0L,
            photosSizeBytes = photosSizeBytes + addedPhotoBytes,
            videosSizeBytes = videosSizeBytes + addedVideoBytes
        )
    }

    private companion object {
        // Shared constants moved to MediaItem
    }
}
