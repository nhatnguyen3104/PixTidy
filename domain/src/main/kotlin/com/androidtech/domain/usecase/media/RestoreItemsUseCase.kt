package com.androidtech.domain.usecase.media

import com.androidtech.domain.model.media.TrashActionResult
import com.androidtech.domain.repository.SettingsRepository
import com.androidtech.domain.repository.TrashRepository
import javax.inject.Inject

/** Restores the selected items from the trash. An empty selection is a no-op success. */
class RestoreItemsUseCase @Inject constructor(
    private val trashRepository: TrashRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(itemIds: List<Long>): TrashActionResult {
        val ids = itemIds.distinct()
        if (ids.isEmpty()) return TrashActionResult.Success
        val result = trashRepository.restoreFromTrash(ids)
        if (result is TrashActionResult.Success || result is TrashActionResult.RequiresUserConsent) {
            settingsRepository.setScanRecommended(true)
        }
        return result
    }
}
