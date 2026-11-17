package com.androidtech.features.ui.screens.splash

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ads.monetization.AdManager
import com.ads.monetization.adplacename.AdPlaceName
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.data.model.aduiresource.AdFullScreenUiResource
import com.androidtech.data.model.aduiresource.getAdPlaceName
import com.androidtech.domain.usecase.FetchAndActiveRemoteConfigsUseCase
import com.androidtech.domain.usecase.settings.GetUserSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SplashViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val adManager: AdManager,
    private val fetchAndActiveConfigsUseCase: FetchAndActiveRemoteConfigsUseCase
): BaseViewModel<SplashEvent, SplashState, SplashEffect>(savedStateHandle) {

    private var isInterAdLoaded = false
    private var isInterAdShowingOrCompleted = false

    override fun createInitializeState(): SplashState = SplashState()

    override fun handleEvent(event: SplashEvent) {
        when(event) {
            is SplashEvent.StartInitialization -> {
                startInitialization()
            }
            is SplashEvent.PreloadAds -> {
                preloadAds(event.activity)
            }
            is SplashEvent.ShowInterAd -> {
                showInterAd(event.activity)
            }
        }
    }

    init {
        // Comment out Interstitial Ad listening for now to focus on Native Ad
        // listenAdFullScreenEvents()
        observeNativeAds()
    }

    private fun startInitialization() {
        viewModelScope.launch {
            val totalSteps = 100
            for (i in 1..totalSteps) {
                delay(25.milliseconds)
                setState { copy(progress = i.toFloat() / totalSteps) }
            }

            navigateToNextScreen()
        }
    }

    private fun preloadAds(activity: Activity) {
        viewModelScope.launch {
            launch { fetchAndActiveConfigsUseCase() }
            // Comment out Interstitial Ad preloading for now
            // adManager.preloadFullScreenAd(activity, AdPlaceName.OPEN_FIRST)
            adManager.preloadBannerNativeAd(placeName = AdPlaceName.OPEN_NATIVE_FIRST)
            val nativeHolder = adManager.getNativeAdHolder(AdPlaceName.OPEN_NATIVE_FIRST)
            setState { copy(nativeHolder = nativeHolder) }
        }
    }

    private fun showInterAd(activity: Activity) {
        viewModelScope.launch {
            isInterAdShowingOrCompleted = true
            // Comment out Interstitial Ad showing for now
            // adManager.showFullScreenAd(activity, AdPlaceName.OPEN_FIRST)
        }
    }

    private fun listenAdFullScreenEvents() {
        /*
        viewModelScope.launch {
            adManager.observeFullScreenAd(AdPlaceName.OPEN_FIRST).collect { uiResource ->
                when(uiResource) {
                    is AdFullScreenUiResource.AdLoaded -> {
                        isInterAdLoaded = true
                        setEffect { SplashEffect.ShowFullScreenAd }
                    }
                    is AdFullScreenUiResource.AdCompleted,
                    is AdFullScreenUiResource.AdNotValidOrLoadFailed -> {
                        isInterAdShowingOrCompleted = true
                        navigateToNextScreen()
                    }
                    else -> Unit
                }
            }
        }
        */
    }

    private fun observeNativeAds() {
        viewModelScope.launch {
            adManager.observeBannerNativeAd(AdPlaceName.OPEN_NATIVE_FIRST).collect {
                val nativeHolder = adManager.getNativeAdHolder(AdPlaceName.OPEN_NATIVE_FIRST)
                setState { copy(nativeHolder = nativeHolder, adLoadTrigger = adLoadTrigger + 1) }
            }
        }
    }

    private fun navigateToNextScreen() {
        viewModelScope.launch {
            val settings = getUserSettingsUseCase().first()
            setState { copy(isOnboardingCompleted = settings.isOnboardingCompleted) }
            if (uiState.value.isOnboardingCompleted) {
                setEffect { SplashEffect.NavigateToHome }
            } else {
                setEffect { SplashEffect.NavigateToLanguage }
            }
        }
    }
}