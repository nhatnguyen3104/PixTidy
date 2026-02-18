package com.androidtech.generalapps.ui.activity

import androidx.lifecycle.SavedStateHandle
import com.androidtech.base.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
): BaseViewModel<MainEvent, MainState, MainEffect>(savedStateHandle) {
    override fun createInitializeState(): MainState = MainState()

    override fun handleEvent(event: MainEvent) {

    }
}