package com.androidtech.generalapps.ui.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.usecase.FetchAndActiveRemoteConfigsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchAndActiveConfigsUseCase: FetchAndActiveRemoteConfigsUseCase
): BaseViewModel<MainEvent, MainState, MainEffect>(savedStateHandle) {
    override fun createInitializeState(): MainState = MainState()

    override fun handleEvent(event: MainEvent) {
        when (event) {
            MainEvent.LoadRemoteConfig -> fetchRemoteConfig()
        }
    }

    private fun fetchRemoteConfig() {
        viewModelScope.launch {
            fetchAndActiveConfigsUseCase()
        }
    }
}