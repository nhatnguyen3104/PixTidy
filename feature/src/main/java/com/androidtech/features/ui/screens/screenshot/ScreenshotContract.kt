package com.androidtech.features.ui.screens.screenshot

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