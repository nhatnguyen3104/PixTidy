package com.androidtech.features.ui.screens.duplicate

import android.content.IntentSender
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.DuplicateGroup

data class DuplicateState(
    val isLoading: Boolean = false,
    val duplicateGroups: List<DuplicateGroup> = emptyList()
): UIState

sealed class DuplicateEvent : UIEvent {
    object MoreClicked : DuplicateEvent()
    object NavigateBackClick : DuplicateEvent()
    data class DeleteClick(val selectedPhotoIds: List<Long>) : DuplicateEvent()
    data class TogglePhotoSelection(val photoId: Long) : DuplicateEvent()
    data class ConfirmCacheCleanup(val photoIds: List<Long>) : DuplicateEvent()
}

sealed class DuplicateEffect : UIEffect {
    object NavigateBack : DuplicateEffect()
    data class ShowSnackbar(val message: String) : DuplicateEffect()
    data class RequestWritePermission(val selectedPhotoIds: List<Long>) : DuplicateEffect()
    data class LaunchIntentSender(val intentSender: IntentSender) : DuplicateEffect()
}
