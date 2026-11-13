package com.androidtech.features.ui.screens.screenshot

import android.content.IntentSender
import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.ScreenshotGroup

data class ScreenshotState(
    val isLoading: Boolean = false,
    val showBackConfirmationDialog: Boolean = false,
    val screenshotGroups: List<ScreenshotGroup> = emptyList(),
    val previewMediaList: List<MediaItem>? = null,
    val previewInitialIndex: Int = 0
): UIState {
    val isSelectionActive: Boolean
        get() = screenshotGroups.any { group ->
            /*group.originalItem.isSelected || */group.screenshotItems.any { it.isSelected }
        }
}

sealed class ScreenshotEvent: UIEvent {
    object SelectAllClicked : ScreenshotEvent()
    object ClearSelectionClicked : ScreenshotEvent()
    object NavigateBackClick : ScreenshotEvent()
    data class DeleteClick(val selectedPhotoIds: List<Long>) : ScreenshotEvent()
    data class PhotoClicked(val photoId: Long) : ScreenshotEvent()
    object ClosePreview : ScreenshotEvent()
    data class PhotoToggleSelection(val photoId: Long) : ScreenshotEvent()
    data class GroupToggleClicked(val groupId: String, val isSelected: Boolean) : ScreenshotEvent()
    data class ConfirmCacheCleanup(val photoIds: List<Long>) : ScreenshotEvent()
    object DismissBackConfirmation : ScreenshotEvent()
    object ConfirmBackClicked : ScreenshotEvent()
}

sealed class ScreenshotEffect: UIEffect {
    object NavigateBack : ScreenshotEffect()
    data class ShowSnackbar(val message: UiText) : ScreenshotEffect()
    data class RequestWritePermission(val selectedPhotoIds: List<Long>) : ScreenshotEffect()
    data class LaunchIntentSender(val intentSender: IntentSender) : ScreenshotEffect()
}