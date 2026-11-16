package com.androidtech.features.ui.screens.largevideo

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
import com.androidtech.domain.usecase.media.GetLatestLargeVideoGroupsUseCase
import com.androidtech.domain.usecase.media.PerformSafeCleanupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LargeVideoViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLatestLargeVideoGroupsUseCase: GetLatestLargeVideoGroupsUseCase,
    private val performSafeCleanupUseCase: PerformSafeCleanupUseCase
): BaseViewModel<LargeVideoEvent, LargeVideoState, LargeVideoEffect>(savedStateHandle) {
    override fun createInitializeState(): LargeVideoState = LargeVideoState()

    init {
        loadLargeVideos()
    }

    private fun loadLargeVideos() {
        getLatestLargeVideoGroupsUseCase(Unit)
            .onEach { uiState ->
                when (uiState) {
                    is UiState.Loading -> setState { copy(isLoading = true) }
                    is UiState.Success -> setState {
                        copy(
                            isLoading = false,
                            largeVideoGroups = uiState.data
                        )
                    }
                    is UiState.Error -> setState { copy(isLoading = false) }
                    else -> {}
                }
            }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: LargeVideoEvent) {
        when (event) {
            is LargeVideoEvent.SelectAllClicked -> {
                setAllSelection(true)
            }
            is LargeVideoEvent.ClearSelectionClicked -> {
                setAllSelection(false)
            }
            is LargeVideoEvent.NavigateBackClick -> {
                handleBack()
            }
            is LargeVideoEvent.DeleteClick -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    setEffect { LargeVideoEffect.RequestWritePermission(event.selectedVideoIds) }
                } else {
                    deleteVideos(event.selectedVideoIds)
                }
            }
            is LargeVideoEvent.VideoClicked -> {
                previewVideo(event.videoId)
            }
            is LargeVideoEvent.ClosePreview -> {
                closePreview()
            }
            is LargeVideoEvent.VideoToggleSelection -> {
                toggleVideoSelection(event.videoId)
            }
            is LargeVideoEvent.GroupToggleClicked -> {
                toggleGroupSelection(event.groupId, event.isSelected)
            }
            is LargeVideoEvent.ConfirmCacheCleanup -> {
                confirmCacheCleanup(event.videoIds)
            }
            is LargeVideoEvent.DismissBackConfirmation -> {
                setState { copy(showBackConfirmationDialog = false) }
            }
            is LargeVideoEvent.ConfirmBackClicked -> {
                setState { copy(showBackConfirmationDialog = false) }
                setEffect { LargeVideoEffect.NavigateBack }
            }
        }
    }

    private fun confirmCacheCleanup(videoIds: List<Long>) {
        viewModelScope.launch {
            val itemsToRemove = mutableListOf<MediaItem>()
            uiState.value.largeVideoGroups.forEach { group ->
                group.videoItems.forEach { item ->
                    if (item.id in videoIds) itemsToRemove.add(item)
                }
            }
            performSafeCleanupUseCase.updateCacheOnly(itemsToRemove)
        }
    }

    private fun deleteVideos(selectedVideoIds: List<Long>) {
        if (selectedVideoIds.isEmpty()) return

        viewModelScope.launch {
            setState { copy(isLoading = true) }

            val itemsToDelete = mutableListOf<MediaItem>()
            uiState.value.largeVideoGroups.forEach { group ->
                group.videoItems.forEach { item ->
                    if (item.id in selectedVideoIds) {
                        itemsToDelete.add(item)
                    }
                }
            }

            performSafeCleanupUseCase(itemsToDelete, confirmed = true)
                .onSuccess { result ->
                    setState { copy(isLoading = false) }
                    val message = UiText.StringResource(R.string.text_notification_clean, result.itemsDeletedCount, result.bytesReclaimed.formatSize())
                    setEffect { LargeVideoEffect.ShowSnackbar(message) }
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
                        setEffect { LargeVideoEffect.LaunchIntentSender(intentSender) }
                    } else {
                        setEffect { LargeVideoEffect.ShowSnackbar(UiText.StringResource(R.string.text_error_clean)) }
                    }
                }
        }
    }

    private fun toggleVideoSelection(videoId: Long) {
        val currentGroups = uiState.value.largeVideoGroups
        val updatedGroups = currentGroups.map { group ->
            when {
                group.videoItems.any { it.id == videoId } -> {
                    group.copy(
                        videoItems = group.videoItems.map { item ->
                            if (item.id == videoId) item.copy(isSelected = !item.isSelected) else item
                        }
                    )
                }
                else -> group
            }
        }
        setState { copy(largeVideoGroups = updatedGroups) }
    }

    private fun previewVideo(videoId: Long) {
        val group = uiState.value.largeVideoGroups.find { group -> group.videoItems.any { it.id == videoId } }
        if (group != null) {
            val index = group.videoItems.indexOfFirst { it.id == videoId }
            if (index != -1) {
                setState {
                    copy(
                        previewMediaList = group.videoItems,
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
        val updatedGroups = uiState.value.largeVideoGroups.map { group ->
            if (group.groupId == groupId) {
                group.copy(
                    videoItems = group.videoItems.map { it.copy(isSelected = isSelected) }
                )
            } else {
                group
            }
        }
        setState { copy(largeVideoGroups = updatedGroups) }
    }

    private fun setAllSelection(isSelected: Boolean) {
        val currentGroups = uiState.value.largeVideoGroups
        val updatedGroups = currentGroups.map { group ->
            group.copy(
                videoItems = group.videoItems.map { item -> item.copy(isSelected = isSelected) }
            )
        }
        setState { copy(largeVideoGroups = updatedGroups) }
    }

    private fun handleBack() {
        val hasSelectedItems = currentState.largeVideoGroups.any { group ->
            group.videoItems.any { it.isSelected }
        }
        if (hasSelectedItems) {
            setState { copy(showBackConfirmationDialog = true) }
        } else {
            setEffect { LargeVideoEffect.NavigateBack }
        }
    }
}
