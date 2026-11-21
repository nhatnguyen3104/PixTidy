package com.androidtech.features.ui.screens.trash

import android.app.Activity
import android.content.IntentSender
import com.ads.monetization.adholder.BannerAdHolder
import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.MediaItem

data class TrashState(
    val isLoading: Boolean = false,
    val showBackConfirmationDialog: Boolean = false,
    val trashMedia: List<MediaItem> = emptyList(),
    val previewMediaList: List<MediaItem>? = null,
    val previewInitialIndex: Int = 0,
    val bannerAdHolder: BannerAdHolder? = null
): UIState {
    val isSelectionActive: Boolean
        get() = trashMedia.any { it.isSelected }
}

sealed class TrashEvent: UIEvent {
    object SelectAllClicked: TrashEvent()
    object ClearSelectionClicked : TrashEvent()
    data class OnItemClick(val id: Long): TrashEvent()
    data class ToggleItem(val id: Long): TrashEvent()
    data class RestoreClick(val selectedIds: List<Long>): TrashEvent()
    data class DeleteClick(val selectedIds: List<Long>): TrashEvent()
    object NavigateBackClick: TrashEvent()
    object ClosePreview : TrashEvent()
    object DismissBackConfirmation : TrashEvent()
    object ConfirmBackClicked : TrashEvent()
    data class PreloadAds(val activity: Activity): TrashEvent()
    data class ShowInterAd(val activity: Activity): TrashEvent()
}

sealed class TrashEffect: UIEffect {
    object NavigateBack: TrashEffect()
    data class ShowSnackbar(val message: UiText) : TrashEffect()
    object ShowFullScreenAd : TrashEffect()
    data class LaunchIntentSender(val intentSender: IntentSender) : TrashEffect()
}
