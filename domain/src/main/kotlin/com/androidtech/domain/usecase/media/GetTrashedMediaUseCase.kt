package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.UIStateUseCase
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.repository.TrashRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Fetches everything currently in the trash bin. */
class GetTrashedMediaUseCase @Inject constructor(
    private val trashRepository: TrashRepository
): UIStateUseCase<Unit, List<MediaItem>>() {

    override fun run(param: Unit): Flow<UiState<List<MediaItem>>> {
        return trashRepository.getTrashedItems().map { items ->
            UiState.Success(items)
        }
    }
}