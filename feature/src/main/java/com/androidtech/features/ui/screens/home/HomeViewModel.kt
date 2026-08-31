package com.androidtech.features.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import com.androidtech.base.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
): BaseViewModel<HomeEvent, HomeState, HomeEffect>(savedStateHandle) {
    override fun createInitializeState(): HomeState = HomeState()

    override fun handleEvent(event: HomeEvent) {
        TODO("Not yet implemented")
    }
}