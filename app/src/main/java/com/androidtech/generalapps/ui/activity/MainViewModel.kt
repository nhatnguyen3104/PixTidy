package com.androidtech.generalapps.ui.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ads.monetization.AdManager
import com.ads.monetization.appopenad.AppOpenAdManager
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.usecase.FetchAndActiveRemoteConfigsUseCase
import com.androidtech.domain.usecase.settings.GetUserSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    //private val fetchAndActiveConfigsUseCase: FetchAndActiveRemoteConfigsUseCase,
    private val adManager: AdManager,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val appOpenAdManager: AppOpenAdManager,
): BaseViewModel<MainEvent, MainState, MainEffect>(savedStateHandle) {

    init {
        observeSettings()
        //fetchRemoteConfig()
        initializeMobileAdsSdk()
    }

    private fun observeSettings() {
        getUserSettingsUseCase()
            .onEach { settings ->
                if (settings.languageCode.isNotEmpty()) {
                    com.androidtech.data.utils.LocaleUtils.applyLocale(settings.languageCode)
                }
                setState { copy(theme = settings.theme, languageCode = settings.languageCode, isOnboardingCompleted = settings.isOnboardingCompleted, isSettingsReady = true) }
            }
            .launchIn(viewModelScope)
    }

    override fun createInitializeState(): MainState = MainState()

    override fun handleEvent(event: MainEvent) {
        when (event) {
            MainEvent.LoadRemoteConfig -> { /*fetchRemoteConfig()*/ }
            MainEvent.LoadUserSettings -> {}
        }
    }

    /*private fun fetchRemoteConfig() {
        viewModelScope.launch {
            fetchAndActiveConfigsUseCase()
        }
    }*/

    private fun initializeMobileAdsSdk() {
        viewModelScope.launch {
            adManager.initializeMobileAdsSdk()
        }
    }

}
