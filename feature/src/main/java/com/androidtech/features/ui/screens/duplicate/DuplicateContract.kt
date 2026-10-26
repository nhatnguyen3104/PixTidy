package com.androidtech.features.ui.screens.duplicate

import android.content.IntentSender
import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.DuplicateGroup

data class DuplicateState(
    val isLoading: Boolean = false,
    val showBackConfirmationDialog: Boolean = false,
    val duplicateGroups: List<DuplicateGroup> = emptyList()
): UIState {
    val isSelectionActive: Boolean
        get() = duplicateGroups.any { group ->
            /*group.originalItem.isSelected || */group.duplicateItems.any { it.isSelected }
        }
}

sealed class DuplicateEvent : UIEvent {
    object SelectAllClicked : DuplicateEvent()
    object ClearSelectionClicked : DuplicateEvent()
    object NavigateBackClick : DuplicateEvent()
    data class DeleteClick(val selectedPhotoIds: List<Long>) : DuplicateEvent()
    data class PhotoToggleSelection(val photoId: Long) : DuplicateEvent()
    data class GroupToggleClicked(val groupId: String, val isSelected: Boolean) : DuplicateEvent()
    data class ConfirmCacheCleanup(val photoIds: List<Long>) : DuplicateEvent()
    object DismissBackConfirmation : DuplicateEvent()
    object ConfirmBackClicked : DuplicateEvent()
}

sealed class DuplicateEffect : UIEffect {
    object NavigateBack : DuplicateEffect()
    data class ShowSnackbar(val message: UiText) : DuplicateEffect()
    data class RequestWritePermission(val selectedPhotoIds: List<Long>) : DuplicateEffect()
    data class LaunchIntentSender(val intentSender: IntentSender) : DuplicateEffect()
}
