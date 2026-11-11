package com.androidtech.domain.usecase.media

import com.androidtech.domain.model.media.TrashActionResult
import com.androidtech.domain.repository.TrashRepository

/** Moves the selected items to the trash. An empty selection is a no-op success. */
class MoveItemsToTrashUseCase(private val trashRepository: TrashRepository) {
    suspend operator fun invoke(itemIds: List<Long>): TrashActionResult {
        val ids = itemIds.distinct()
        if (ids.isEmpty()) return TrashActionResult.Success
        return trashRepository.moveToTrash(ids)
    }
}