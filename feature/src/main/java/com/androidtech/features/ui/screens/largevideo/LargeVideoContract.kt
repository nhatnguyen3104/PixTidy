package com.androidtech.features.ui.screens.largevideo

import android.content.IntentSender
import com.ads.monetization.adholder.BannerAdHolder
import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.LargeVideoGroup
import com.androidtech.domain.model.media.MediaItem

data class LargeVideoState(
    val isLoading: Boolean = false,
    val showBackConfirmationDialog: Boolean = false,
    val largeVideoGroups: List<LargeVideoGroup> = emptyList(),
    val previewMediaList: List<MediaItem>? = null,
    val previewInitialIndex: Int = 0,
    val bannerAdHolder: BannerAdHolder? = null
): UIState {
    val isSelectionActive: Boolean
        get() = largeVideoGroups.any { group ->
            group.videoItems.any { it.isSelected }
        }
}

sealed class LargeVideoEvent: UIEvent {
    object SelectAllClicked : LargeVideoEvent()
    object ClearSelectionClicked : LargeVideoEvent()
    object NavigateBackClick : LargeVideoEvent()
    data class DeleteClick(val selectedVideoIds: List<Long>) : LargeVideoEvent()
    data class VideoClicked(val videoId: Long) : LargeVideoEvent()
    object ClosePreview : LargeVideoEvent()
    data class VideoToggleSelection(val videoId: Long) : LargeVideoEvent()
    data class GroupToggleClicked(val groupId: String, val isSelected: Boolean) : LargeVideoEvent()
    data class ConfirmCacheCleanup(val videoIds: List<Long>) : LargeVideoEvent()
    object DismissBackConfirmation : LargeVideoEvent()
    object ConfirmBackClicked : LargeVideoEvent()
}

sealed class LargeVideoEffect: UIEffect {
    object NavigateBack : LargeVideoEffect()
    data class ShowSnackbar(val message: UiText) : LargeVideoEffect()
    data class RequestWritePermission(val selectedVideoIds: List<Long>) : LargeVideoEffect()
    data class LaunchIntentSender(val intentSender: IntentSender) : LargeVideoEffect()
}
