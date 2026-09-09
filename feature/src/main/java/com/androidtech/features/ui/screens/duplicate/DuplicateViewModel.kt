package com.androidtech.features.ui.screens.duplicate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.usecase.media.GetLatestDuplicateGroupsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class DuplicateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLatestDuplicateGroupsUseCase: GetLatestDuplicateGroupsUseCase
): BaseViewModel<DuplicateEvent, DuplicateState, DuplicateEffect>(savedStateHandle) {
    override fun createInitializeState(): DuplicateState = DuplicateState()

    init {
        loadDuplicates()
    }

    private fun loadDuplicates() {
        getLatestDuplicateGroupsUseCase(Unit)
            .onEach { uiState ->
                when (uiState) {
                    is UiState.Loading -> setState { copy(isLoading = true) }
                    is UiState.Success -> setState {
                        copy(
                            isLoading = false,
                            duplicateGroups = uiState.data
                        )
                    }
                    is UiState.Error -> setState { copy(isLoading = false) }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: DuplicateEvent) {
        when (event) {
            is DuplicateEvent.MoreClicked -> {
                //setEffect(DuplicateEffect.NavigateBack)
            }
            is DuplicateEvent.NavigateBackClick -> {
                setEffect { DuplicateEffect.NavigateBack }
            }
            is DuplicateEvent.DeleteClick -> {
                //deletePhotos(event.selectedPhotoIds)
            }
            is DuplicateEvent.TogglePhotoSelection -> {
                //togglePhotoSelection(event.photoId)
            }
        }
    }
}