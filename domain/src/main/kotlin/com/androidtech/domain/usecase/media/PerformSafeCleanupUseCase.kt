package com.androidtech.domain.usecase.media

import com.androidtech.domain.repository.MediaStoreRepository
import javax.inject.Inject

/**
 * Deletes user-approved items. Deliberately requires an explicit [confirmed] flag rather than
 * trusting the caller's UI state — this is the last line of defense before irreversible (or
 * system-trash-bin-reversible, depending on API level) deletion.
 */
class PerformSafeCleanupUseCase @Inject constructor(
    private val mediaStoreRepository: MediaStoreRepository
) {

    suspend operator fun invoke(uris: List<String>, confirmed: Boolean): Result<Unit> {
        if (!confirmed) {
            return Result.failure(
                IllegalStateException("Cleanup requires explicit user confirmation before deletion")
            )
        }
        if (uris.isEmpty()) return Result.success(Unit)

        return mediaStoreRepository.deleteItems(uris)
    }
}