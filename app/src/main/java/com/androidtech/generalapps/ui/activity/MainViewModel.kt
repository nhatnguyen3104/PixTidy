package com.androidtech.generalapps.ui.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.usecase.FetchAndActiveRemoteConfigsUseCase
//import com.androidtech.domain.usecase.settings.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fetchAndActiveConfigsUseCase: FetchAndActiveRemoteConfigsUseCase,
    //private val observeSettingsUseCase: ObserveSettingsUseCase
): BaseViewModel<MainEvent, MainState, MainEffect>(savedStateHandle) {

    init {
        observeSettings()
        fetchRemoteConfig()
    }

    private fun observeSettings() {
        /*observeSettingsUseCase()
            .onEach { settings ->
                setState { copy(isDarkMode = settings.isDarkMode) }
            }
            .launchIn(viewModelScope)*/
    }

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