package com.androidtech.features.ui.screens.similar

import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.utils.formatSize
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.data.repository.RequiresUserConfirmationException
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.usecase.media.GetLatestSimilarGroupsUseCase
import com.androidtech.domain.usecase.media.PerformSafeCleanupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimilarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLatestSimilarGroupsUseCase: GetLatestSimilarGroupsUseCase,
    private val performSafeCleanupUseCase: PerformSafeCleanupUseCase
): BaseViewModel<SimilarEvent, SimilarState, SimilarEffect>(savedStateHandle) {
    override fun createInitializeState(): SimilarState = SimilarState()

    init {
        loadSimilarPhotos()
    }

    private fun loadSimilarPhotos() {
        getLatestSimilarGroupsUseCase(Unit)
            .onEach { result ->
                when (result) {
                    is UiState.Loading -> setState { copy(isLoading = true) }
                    is UiState.Success -> setState {
                        copy(
                            isLoading = false,
                            similarGroups = result.data
                        )
                    }
                    is UiState.Error -> setState { copy(isLoading = false) }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: SimilarEvent) {
        when (event) {
            is SimilarEvent.MoreClicked -> {
                // handle more clicked
            }
            is SimilarEvent.NavigateBackClick -> {
                setEffect { SimilarEffect.NavigateBack }
            }
            is SimilarEvent.DeleteClick -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    setEffect { SimilarEffect.RequestWritePermission(event.selectedPhotoIds) }
                } else {
                    deletePhotos(event.selectedPhotoIds)
                }
            }
            is SimilarEvent.TogglePhotoSelection -> {
                togglePhotoSelection(event.photoId)
            }
            is SimilarEvent.ConfirmCacheCleanup -> {
                confirmCacheCleanup(event.photoIds)
            }
        }
    }

    private fun confirmCacheCleanup(photoIds: List<Long>) {
        viewModelScope.launch {
            val itemsToRemove = mutableListOf<MediaItem>()
            uiState.value.similarGroups.forEach { group ->
                group.items.forEach { item ->
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

            val itemsToDelete = mutableListOf<MediaItem>()
            uiState.value.similarGroups.forEach { group ->
                group.items.forEach { item ->
                    if (item.id in selectedPhotoIds) {
                        itemsToDelete.add(item)
                    }
                }
            }

            performSafeCleanupUseCase(itemsToDelete, confirmed = true)
                .onSuccess { result ->
                    setState { copy(isLoading = false) }
                    val message = "Đã dọn dẹp ${result.itemsDeletedCount} tệp, giải phóng ${result.bytesReclaimed.formatSize()}"
                    setEffect { SimilarEffect.ShowSnackbar(message) }
                }
                .onFailure { error ->
                    setState { copy(isLoading = false) }
                    if (error is com.androidtech.data.repository.RequiresUserConfirmationException) {
                        setEffect { SimilarEffect.LaunchIntentSender(error.intentSender) }
                    } else {
                        setEffect { SimilarEffect.ShowSnackbar("Lỗi khi xóa: ${error.message}") }
                    }
                }
        }
    }

    private fun togglePhotoSelection(photoId: Long) {
        val currentGroups = uiState.value.similarGroups
        val updatedGroups = currentGroups.map { group ->
            if (group.items.any { it.id == photoId }) {
                group.copy(
                    items = group.items.map { item ->
                        if (item.id == photoId) item.copy(isSelected = !item.isSelected) else item
                    }
                )
            } else {
                group
            }
        }
        setState { copy(similarGroups = updatedGroups) }
    }
}
