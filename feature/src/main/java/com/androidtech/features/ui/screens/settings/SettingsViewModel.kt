package com.androidtech.features.ui.screens.settings

import androidx.lifecycle.SavedStateHandle
import com.androidtech.base.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
): BaseViewModel<SettingsEvent, SettingsState, SettingsEffect>(savedStateHandle) {
    override fun createInitializeState(): SettingsState = SettingsState()

    override fun handleEvent(event: SettingsEvent) {
        TODO("Not yet implemented")
    }
}