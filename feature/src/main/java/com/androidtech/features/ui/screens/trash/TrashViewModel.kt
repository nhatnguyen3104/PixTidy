package com.androidtech.features.ui.screens.trash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.usecase.media.GetTrashedMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTrashedMediaUseCase: GetTrashedMediaUseCase
): BaseViewModel<TrashEvent, TrashState, TrashEffect>(savedStateHandle) {
    override fun createInitializeState(): TrashState = TrashState()

    init {
        loadTrashMedia()
    }

    private fun loadTrashMedia() {
        getTrashedMediaUseCase(Unit).onEach { result ->
            when (result) {
                is UiState.Loading -> setState { copy(isLoading = true) }
                is UiState.Success -> setState {
                    copy(
                        isLoading = false,
                        trashMedia = result.data
                    )
                }
                is UiState.Error -> setState { copy(isLoading = false) }
                else -> {}
            }
        }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: TrashEvent) {
        when (event) {
            is TrashEvent.SelectAllClicked -> {
                setAllSelection(true)
            }
            is TrashEvent.ClearSelectionClicked -> {
                setAllSelection(false)
            }
            is TrashEvent.OnItemClick -> {
                previewPhoto(event.id)
            }
            is TrashEvent.ToggleItem -> {
                toggleItemSelection(event.id)
            }
            is TrashEvent.RestoreClick -> {
                restoreItems(event.selectedIds)
            }
            is TrashEvent.DeleteClick -> {
                deleteItems(event.selectedIds)
            }
            is TrashEvent.NavigateBackClick -> {
                handleBack()
            }
            is TrashEvent.ClosePreview -> {
                closePreview()
            }
        }
    }

    private fun toggleItemSelection(itemId: Long) {
        val items = uiState.value.trashMedia
        val updatedItems = items.map { item ->
            if (item.id == itemId) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
        setState { copy(trashMedia = updatedItems) }
    }

    private fun restoreItems(selectedItems: List<Long>) {

    }

    private fun deleteItems(selectedItems: List<Long>) {

    }

    private fun previewPhoto(itemId: Long) {
        val items = uiState.value.trashMedia
        val index = items.indexOfFirst { it.id == itemId }
        if (index != -1) {
            setState {
                copy(
                    previewMediaList = items,
                    previewInitialIndex = index
                )
            }
        }
    }

    private fun closePreview() {
        setState { copy(previewMediaList = null) }
    }

    private fun setAllSelection(isSelected: Boolean) {
        val currentItems = uiState.value.trashMedia
        val updatedItems = currentItems.map { item ->
            item.copy(isSelected = isSelected)
        }
        setState { copy(trashMedia = updatedItems) }
    }

    private fun handleBack() {
        val hasSelectedItems = currentState.trashMedia.any { it.isSelected }
        if (hasSelectedItems) {
            setState { copy(showBackConfirmationDialog = true) }
        } else {
            setEffect { TrashEffect.NavigateBack }
        }
    }
}