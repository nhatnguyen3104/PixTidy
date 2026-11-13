package com.androidtech.features.ui.screens.screenshot

import android.app.PendingIntent
import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.R
import com.androidtech.base.utils.UiText
import com.androidtech.base.utils.formatSize
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.data.repository.RequiresUserConfirmationException
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.RequiresUserConsentException
import com.androidtech.domain.usecase.media.GetLatestScreenshotGroupsUseCase
import com.androidtech.domain.usecase.media.PerformSafeCleanupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScreenshotViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLatestScreenshotGroupsUseCase: GetLatestScreenshotGroupsUseCase,
    private val performSafeCleanupUseCase: PerformSafeCleanupUseCase
): BaseViewModel<ScreenshotEvent, ScreenshotState, ScreenshotEffect>(savedStateHandle) {
    override fun createInitializeState(): ScreenshotState = ScreenshotState()

    init {
        loadScreenshot()
    }

    private fun loadScreenshot() {
        getLatestScreenshotGroupsUseCase(Unit)
            .onEach { uiState ->
                when (uiState) {
                    is UiState.Loading -> setState { copy(isLoading = true) }
                    is UiState.Success -> setState {
                        copy(
                            isLoading = false,
                            screenshotGroups = uiState.data
                        )
                    }
                    is UiState.Error -> setState { copy(isLoading = false) }
                    else -> {}
                }
            }.launchIn(viewModelScope)
    }
    override fun handleEvent(event: ScreenshotEvent) {
        when (event) {
            is ScreenshotEvent.SelectAllClicked -> {
                setAllSelection(true)
            }
            is ScreenshotEvent.ClearSelectionClicked -> {
                setAllSelection(false)
            }
            is ScreenshotEvent.NavigateBackClick -> {
                handleBack()
            }
            is ScreenshotEvent.DeleteClick -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    setEffect { ScreenshotEffect.RequestWritePermission(event.selectedPhotoIds) }
                } else {
                    deletePhotos(event.selectedPhotoIds)
                }
            }
            is ScreenshotEvent.PhotoClicked -> {
                previewPhoto(event.photoId)
            }
            is ScreenshotEvent.ClosePreview -> {
                closePreview()
            }
            is ScreenshotEvent.PhotoToggleSelection -> {
                togglePhotoSelection(event.photoId)
            }
            is ScreenshotEvent.GroupToggleClicked -> {
                toggleGroupSelection(event.groupId, event.isSelected)
            }
            is ScreenshotEvent.ConfirmCacheCleanup -> {
                confirmCacheCleanup(event.photoIds)
            }
            is ScreenshotEvent.DismissBackConfirmation -> {
                setState { copy(showBackConfirmationDialog = false) }
            }
            is ScreenshotEvent.ConfirmBackClicked -> {
                setState { copy(showBackConfirmationDialog = false) }
                setEffect { ScreenshotEffect.NavigateBack }
            }
        }
    }

    private fun confirmCacheCleanup(photoIds: List<Long>) {
        viewModelScope.launch {
            val itemsToRemove = mutableListOf<MediaItem>()
            uiState.value.screenshotGroups.forEach { group ->
                group.screenshotItems.forEach { item ->
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
            uiState.value.screenshotGroups.forEach { group ->
                group.screenshotItems.forEach { item ->
                    if (item.id in selectedPhotoIds) {
                        itemsToDelete.add(item)
                    }
                }
            }

            performSafeCleanupUseCase(itemsToDelete, confirmed = true)
                .onSuccess { result ->
                    setState { copy(isLoading = false) }
                    val message = UiText.StringResource(R.string.text_notification_clean, result.itemsDeletedCount, result.bytesReclaimed.formatSize())
                    setEffect { ScreenshotEffect.ShowSnackbar(message) }
                }
                .onFailure { error ->
                    setState { copy(isLoading = false) }
                    val intentSender = when (error) {
                        is RequiresUserConfirmationException -> error.intentSender
                        is RequiresUserConsentException -> {
                            (error.actionSender as? PendingIntent)?.intentSender
                                ?: (error.actionSender as? IntentSender)
                        }
                        else -> null
                    }
                    if (intentSender != null) {
                        setEffect { ScreenshotEffect.LaunchIntentSender(intentSender) }
                    } else {
                        setEffect { ScreenshotEffect.ShowSnackbar(UiText.StringResource(R.string.text_error_clean)) }
                    }
                }
        }
    }

    private fun togglePhotoSelection(photoId: Long) {
        val currentGroups = uiState.value.screenshotGroups
        val updatedGroups = currentGroups.map { group ->
            when {
                group.screenshotItems.any { it.id == photoId } -> {
                    group.copy(
                        screenshotItems = group.screenshotItems.map { item ->
                            if (item.id == photoId) item.copy(isSelected = !item.isSelected) else item
                        }
                    )
                }
                else -> group
            }
        }
        setState { copy(screenshotGroups = updatedGroups) }
    }

    private fun previewPhoto(photoId: Long) {
        val group = uiState.value.screenshotGroups.find { group -> group.screenshotItems.any { it.id == photoId } }
        if (group != null) {
            val index = group.screenshotItems.indexOfFirst { it.id == photoId }
            if (index != -1) {
                setState {
                    copy(
                        previewMediaList = group.screenshotItems,
                        previewInitialIndex = index
                    )
                }
            }
        }
    }

    private fun closePreview() {
        setState { copy(previewMediaList = null) }
    }

    private fun toggleGroupSelection(groupId: String, isSelected: Boolean) {
        val updatedGroups = uiState.value.screenshotGroups.map { group ->
            if (group.groupId == groupId) {
                group.copy(
                    screenshotItems = group.screenshotItems.map { it.copy(isSelected = isSelected) }
                )
            } else {
                group
            }
        }
        setState { copy(screenshotGroups = updatedGroups) }
    }

    private fun setAllSelection(isSelected: Boolean) {
        val currentGroups = uiState.value.screenshotGroups
        val updatedGroups = currentGroups.map { group ->
            group.copy(
                //originalItem = group.originalItem.copy(isSelected = isSelected),
                screenshotItems = group.screenshotItems.map { item -> item.copy(isSelected = isSelected) }
            )
        }
        setState { copy(screenshotGroups = updatedGroups) }
    }

    private fun handleBack() {
        val hasSelectedItems = currentState.screenshotGroups.any { group ->
            group.screenshotItems.any { it.isSelected }
        }
        if (hasSelectedItems) {
            setState { copy(showBackConfirmationDialog = true) }
        } else {
            setEffect { ScreenshotEffect.NavigateBack }
        }
    }
}