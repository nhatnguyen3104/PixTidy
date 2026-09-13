package com.androidtech.features.ui.screens.similar

import android.content.IntentSender
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.SimilarGroup

data class SimilarState(
    val isLoading: Boolean = false,
    val similarGroups: List<SimilarGroup> = emptyList()
): UIState

sealed class SimilarEvent : UIEvent {
    object MoreClicked : SimilarEvent()
    object NavigateBackClick : SimilarEvent()
    data class DeleteClick(val selectedPhotoIds: List<Long>) : SimilarEvent()
    data class TogglePhotoSelection(val photoId: Long) : SimilarEvent()
    data class ConfirmCacheCleanup(val photoIds: List<Long>) : SimilarEvent()
}

sealed class SimilarEffect : UIEffect {
    object NavigateBack : SimilarEffect()
    data class ShowSnackbar(val message: String) : SimilarEffect()
    data class RequestWritePermission(val selectedPhotoIds: List<Long>) : SimilarEffect()
    data class LaunchIntentSender(val intentSender: IntentSender) : SimilarEffect()
}
