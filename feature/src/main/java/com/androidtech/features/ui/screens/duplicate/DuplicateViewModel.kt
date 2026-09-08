package com.androidtech.features.ui.screens.duplicate

import androidx.lifecycle.SavedStateHandle
import com.androidtech.base.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DuplicateViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
): BaseViewModel<DuplicateEvent, DuplicateState, DuplicateEffect>(savedStateHandle) {
    override fun createInitializeState(): DuplicateState = DuplicateState()

    override fun handleEvent(event: DuplicateEvent) {
        when (event) {
            is DuplicateEvent.MoreClicked -> {
                //setEffect(DuplicateEffect.NavigateBack)
            }
            is DuplicateEvent.NavigateBackClick -> {
                setEffect { DuplicateEffect.NavigateBack }
            }
            is DuplicateEvent.DeleteClick -> {
                //deletePhotos(event.selectedPhotoIds)
            }
            is DuplicateEvent.TogglePhotoSelection -> {
                //togglePhotoSelection(event.photoId)
            }
        }
    }
}