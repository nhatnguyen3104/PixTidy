package com.androidtech.features.ui.screens.duplicate

import android.os.Build
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.R
import com.androidtech.base.utils.UiText
import com.androidtech.base.utils.formatSize
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.data.repository.RequiresUserConfirmationException
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.usecase.media.GetLatestDuplicateGroupsUseCase
import com.androidtech.domain.usecase.media.PerformSafeCleanupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DuplicateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLatestDuplicateGroupsUseCase: GetLatestDuplicateGroupsUseCase,
    private val performSafeCleanupUseCase: PerformSafeCleanupUseCase
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
            is DuplicateEvent.SelectAllClicked -> {
                setAllSelection(true)
            }
            is DuplicateEvent.ClearSelectionClicked -> {
                setAllSelection(false)
            }
            is DuplicateEvent.NavigateBackClick -> {
                setEffect { DuplicateEffect.NavigateBack }
            }
            is DuplicateEvent.DeleteClick -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    setEffect { DuplicateEffect.RequestWritePermission(event.selectedPhotoIds) }
                } else {
                    deletePhotos(event.selectedPhotoIds)
                }
            }
            is DuplicateEvent.TogglePhotoSelection -> {
                togglePhotoSelection(event.photoId)
            }
            is DuplicateEvent.ConfirmCacheCleanup -> {
                confirmCacheCleanup(event.photoIds)
            }
        }
    }

    private fun confirmCacheCleanup(photoIds: List<Long>) {
        viewModelScope.launch {
            val itemsToRemove = mutableListOf<MediaItem>()
            uiState.value.duplicateGroups.forEach { group ->
                if (group.originalItem.id in photoIds) itemsToRemove.add(group.originalItem)
                group.duplicateItems.forEach { item ->
                    if (item.id in photoIds) itemsToRemove.add(item)
                }
            }
            performSafeCleanupUseCase.updateCacheOnly(itemsToRemove)
        }
    }

    private fun deletePhotos(selectedPhotoIds: List<Long>) {
        if (selectedPhotoIds.isEmpty()) return

        viewModelScope.launch {
            setState { copy(isLoading = true) }

            // Find MediaItems with ID selected
            val itemsToDelete = mutableListOf<MediaItem>()
            uiState.value.duplicateGroups.forEach { group ->
                if (group.originalItem.id in selectedPhotoIds) {
                    itemsToDelete.add(group.originalItem)
                }
                group.duplicateItems.forEach { item ->
                    if (item.id in selectedPhotoIds) {
                        itemsToDelete.add(item)
                    }
                }
            }

            performSafeCleanupUseCase(itemsToDelete, confirmed = true)
                .onSuccess { result ->
                    setState { copy(isLoading = false) }
                    val message = UiText.StringResource(R.string.text_notification_clean, result.itemsDeletedCount, result.bytesReclaimed.formatSize())
                    setEffect { DuplicateEffect.ShowSnackbar(message) }
                }
                .onFailure { error ->
                    setState { copy(isLoading = false) }
                    if (error is RequiresUserConfirmationException) {
                        setEffect { DuplicateEffect.LaunchIntentSender(error.intentSender) }
                    } else {
                        setEffect { DuplicateEffect.ShowSnackbar(UiText.StringResource(R.string.text_error_clean)) }
                    }
                }
        }
    }

    private fun togglePhotoSelection(photoId: Long) {
        val currentGroups = uiState.value.duplicateGroups
        val updatedGroups = currentGroups.map { group ->
            when {
                group.originalItem.id == photoId -> {
                    group.copy(originalItem = group.originalItem.copy(isSelected = !group.originalItem.isSelected))
                }
                group.duplicateItems.any { it.id == photoId } -> {
                    group.copy(
                        duplicateItems = group.duplicateItems.map { item ->
                            if (item.id == photoId) item.copy(isSelected = !item.isSelected) else item
                        }
                    )
                }
                else -> group
            }
        }
        setState { copy(duplicateGroups = updatedGroups) }
    }

    private fun setAllSelection(isSelected: Boolean) {
        val currentGroups = uiState.value.duplicateGroups
        val updatedGroups = currentGroups.map { group ->
            group.copy(
                //originalItem = group.originalItem.copy(isSelected = isSelected),
                duplicateItems = group.duplicateItems.map { item -> item.copy(isSelected = isSelected) }
            )
        }
        setState { copy(duplicateGroups = updatedGroups) }
    }
}