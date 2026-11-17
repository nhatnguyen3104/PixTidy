package com.androidtech.features.ui.screens.similar

import android.content.IntentSender
import com.ads.monetization.adholder.BannerAdHolder
import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.SimilarGroup
import com.androidtech.features.ui.screens.duplicate.DuplicateEvent

data class SimilarState(
    val isLoading: Boolean = false,
    val showBackConfirmationDialog: Boolean = false,
    val similarGroups: List<SimilarGroup> = emptyList(),
    val previewMediaList: List<MediaItem>? = null,
    val previewInitialIndex: Int = 0,
    val bannerAdHolder: BannerAdHolder? = null
): UIState {
    val isSelectionActive: Boolean
        get() = similarGroups.any { group ->
            /*group.originalItem.isSelected || */group.items.any { it.isSelected }
        }
}

sealed class SimilarEvent : UIEvent {
    object SelectAllClicked : SimilarEvent()
    object ClearSelectionClicked : SimilarEvent()
    object NavigateBackClick : SimilarEvent()
    data class DeleteClick(val selectedPhotoIds: List<Long>) : SimilarEvent()
    data class PhotoClicked(val photoId: Long) : SimilarEvent()
    object ClosePreview : SimilarEvent()
    data class PhotoToggleSelection(val photoId: Long) : SimilarEvent()
    data class GroupToggleClicked(val groupId: String, val isSelected: Boolean) : SimilarEvent()
    data class ConfirmCacheCleanup(val photoIds: List<Long>) : SimilarEvent()
    object DismissBackConfirmation : SimilarEvent()
    object ConfirmBackClicked : SimilarEvent()
}

sealed class SimilarEffect : UIEffect {
    object NavigateBack : SimilarEffect()
    data class ShowSnackbar(val message: UiText) : SimilarEffect()
    data class RequestWritePermission(val selectedPhotoIds: List<Long>) : SimilarEffect()
    data class LaunchIntentSender(val intentSender: IntentSender) : SimilarEffect()
}
