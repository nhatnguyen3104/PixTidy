package com.androidtech.domain.usecase.media

import com.androidtech.domain.model.media.TrashActionResult
import com.androidtech.domain.repository.TrashRepository

/** Permanently deletes the selected items. An empty selection is a no-op success. */
class DeletePermanentlyUseCase(private val trashRepository: TrashRepository) {
    suspend operator fun invoke(itemIds: List<Long>): TrashActionResult {
        val ids = itemIds.distinct()
        if (ids.isEmpty()) return TrashActionResult.Success
        return trashRepository.deletePermanently(ids)
    }
}