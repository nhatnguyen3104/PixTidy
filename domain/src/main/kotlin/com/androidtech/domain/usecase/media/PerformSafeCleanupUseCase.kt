package com.androidtech.domain.usecase.media

import com.androidtech.domain.model.media.CleanupResult
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.RequiresUserConsentException
import com.androidtech.domain.model.media.TrashActionResult
import com.androidtech.domain.repository.MediaStoreRepository
import com.androidtech.domain.repository.ScanResultsRepository
import com.androidtech.domain.repository.TrashRepository
import javax.inject.Inject

/**
 * Moves user-approved items to system trash (or permanently deletes them on older devices)
 * and invalidates the local scan cache to maintain consistency.
 *
 * Deliberately requires an explicit [confirmed] flag rather than trusting the caller's UI state
 * — this is the last line of defense before items are moved to trash or deleted.
 */
class PerformSafeCleanupUseCase @Inject constructor(
    private val trashRepository: TrashRepository,
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

        val itemIds = items.map { it.id }
        val totalBytes = items.sumOf { it.size }

        return when (val actionResult = trashRepository.moveToTrash(itemIds)) {
            is TrashActionResult.Success -> {
                scanResultsRepository.updateResultsAfterDeletion(items)
                Result.success(
                    CleanupResult(
                        itemsDeletedCount = items.size,
                        bytesReclaimed = totalBytes
                    )
                )
            }
            is TrashActionResult.RequiresUserConsent -> {
                Result.failure(RequiresUserConsentException(actionResult.actionSender))
            }
            is TrashActionResult.Error -> {
                if (actionResult.throwable is UnsupportedOperationException) {
                    performFallbackPermanentDelete(items, totalBytes)
                } else {
                    Result.failure(actionResult.throwable)
                }
            }
        }
    }

    private suspend fun performFallbackPermanentDelete(
        items: List<MediaItem>,
        totalBytes: Long
    ): Result<CleanupResult> {
        val uris = items.map { it.uri }
        return mediaStoreRepository.deleteItems(uris).map {
            scanResultsRepository.updateResultsAfterDeletion(items)
            CleanupResult(
                itemsDeletedCount = items.size,
                bytesReclaimed = totalBytes
            )
        }
    }

    /**
     * Updates the database cache directly. Used when physical deletion or moving to trash
     * was already performed successfully by the Android system via IntentSender confirmation dialog.
     */
    suspend fun updateCacheOnly(items: List<MediaItem>) {
        scanResultsRepository.updateResultsAfterDeletion(items)
    }
}
