package com.androidtech.features.ui.screens.ocrdodument

import android.content.IntentSender
import com.ads.monetization.adholder.BannerAdHolder
import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.MediaItem

data class OcrDocumentState(
    val isLoading: Boolean = false,
    val showBackConfirmationDialog: Boolean = false,
    val ocrDocumentLists: List<MediaItem> = emptyList(),
    val previewMediaList: List<MediaItem>? = null,
    val previewInitialIndex: Int = 0,
    val bannerAdHolder: BannerAdHolder? = null
): UIState {
    val isSelectionActive: Boolean
        get() = ocrDocumentLists.any { it.isSelected }
}

sealed class OcrDocumentEvent: UIEvent {
    object SelectAllClicked : OcrDocumentEvent()
    object ClearSelectionClicked : OcrDocumentEvent()
    object NavigateBackClick : OcrDocumentEvent()
    data class DeleteClick(val selectedPhotoIds: List<Long>) : OcrDocumentEvent()
    data class PhotoClicked(val photoId: Long) : OcrDocumentEvent()
    object ClosePreview : OcrDocumentEvent()
    data class PhotoToggleSelection(val photoId: Long) : OcrDocumentEvent()
    data class ConfirmCacheCleanup(val photoIds: List<Long>) : OcrDocumentEvent()
    object DismissBackConfirmation : OcrDocumentEvent()
    object ConfirmBackClicked : OcrDocumentEvent()
}

sealed class OcrDocumentEffect: UIEffect {
    object NavigateBack : OcrDocumentEffect()
    data class ShowSnackbar(val message: UiText) : OcrDocumentEffect()
    data class RequestWritePermission(val selectedPhotoIds: List<Long>) : OcrDocumentEffect()
    data class LaunchIntentSender(val intentSender: IntentSender) : OcrDocumentEffect()
}