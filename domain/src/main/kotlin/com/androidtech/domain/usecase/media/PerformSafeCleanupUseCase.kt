package com.androidtech.domain.usecase.media

import com.androidtech.domain.model.media.CleanupResult
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.repository.MediaStoreRepository
import com.androidtech.domain.repository.ScanResultsRepository
import javax.inject.Inject

/**
 * Deletes user-approved items and invalidates the local scan cache to maintain consistency.
 *
 * Deliberately requires an explicit [confirmed] flag rather than trusting the caller's UI state
 * — this is the last line of defense before irreversible deletion.
 */
class PerformSafeCleanupUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository,
    private val scanResultsRepository: ScanResultsRepository
) {

    suspend operator fun invoke(items: List<MediaItem>, confirmed: Boolean): Result<CleanupResult> {
        if (!confirmed) {
            return Result.failure(
                IllegalStateException("Cleanup requires explicit user confirmation before deletion")
            )
        }
        if (items.isEmpty()) return Result.success(CleanupResult(0, 0L))

        val uris = items.map { it.uri }
        val totalBytes = items.sumOf { it.size }

        return mediaStoreRepository.deleteItems(uris).map {
            // After successful physical deletion, update the cached results incrementally
            scanResultsRepository.updateResultsAfterDeletion(items)

            CleanupResult(
                itemsDeletedCount = items.size,
                bytesReclaimed = totalBytes
            )
        }
    }

    /**
     * Updates the database cache directly. Used when physical deletion was already 
     * performed successfully by the Android system via IntentSender confirmation dialog.
     */
    suspend fun updateCacheOnly(items: List<MediaItem>) {
        scanResultsRepository.updateResultsAfterDeletion(items)
    }
}
