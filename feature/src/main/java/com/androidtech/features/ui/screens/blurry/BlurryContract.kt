package com.androidtech.features.ui.screens.blurry

import android.content.IntentSender
import com.ads.monetization.adholder.BannerAdHolder
import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.MediaItem

data class BlurryState(
    val isLoading: Boolean = false,
    val showBackConfirmationDialog: Boolean = false,
    val blurryLists: List<MediaItem> = emptyList(),
    val previewMediaList: List<MediaItem>? = null,
    val previewInitialIndex: Int = 0,
    val bannerAdHolder: BannerAdHolder? = null
): UIState {
    val isSelectionActive: Boolean
        get() = blurryLists.any { it.isSelected }
}

sealed class BlurryEvent: UIEvent {
    object SelectAllClicked : BlurryEvent()
    object ClearSelectionClicked : BlurryEvent()
    object NavigateBackClick : BlurryEvent()
    data class DeleteClick(val selectedPhotoIds: List<Long>) : BlurryEvent()
    data class PhotoClicked(val photoId: Long) : BlurryEvent()
    object ClosePreview : BlurryEvent()
    data class PhotoToggleSelection(val photoId: Long) : BlurryEvent()
    data class ConfirmCacheCleanup(val photoIds: List<Long>) : BlurryEvent()
    object DismissBackConfirmation : BlurryEvent()
    object ConfirmBackClicked : BlurryEvent()
}

sealed class BlurryEffect: UIEffect {
    object NavigateBack : BlurryEffect()
    data class ShowSnackbar(val message: UiText) : BlurryEffect()
    data class RequestWritePermission(val selectedPhotoIds: List<Long>) : BlurryEffect()
    data class LaunchIntentSender(val intentSender: IntentSender) : BlurryEffect()
}
