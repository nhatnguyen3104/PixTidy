package com.androidtech.features.ui.screens.onboarding

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ads.monetization.AdManager
import com.ads.monetization.adplacename.AdPlaceName
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.data.model.aduiresource.AdFullScreenUiResource
import com.androidtech.domain.usecase.FetchAndActiveRemoteConfigsUseCase
import com.androidtech.domain.usecase.settings.SetOnboardingCompleted
import com.androidtech.features.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val setOnboardingCompleted: SetOnboardingCompleted,
    private val adManager: AdManager,
    private val fetchAndActiveConfigsUseCase: FetchAndActiveRemoteConfigsUseCase
): BaseViewModel<OnboardingEvent,OnboardingState, OnboardingEffect>(savedStateHandle) {
    private var isFinishingOnboarding = false

    override fun createInitializeState(): OnboardingState = OnboardingState()

    override fun handleEvent(event: OnboardingEvent) {
        when(event) {
            is OnboardingEvent.Initialize -> initialize()
            is OnboardingEvent.ClickNext -> clickNext(event.page)
            is OnboardingEvent.PreloadAds -> preLoadAds(event.activity)
            is OnboardingEvent.ShowInterAd -> showInterAd(event.activity)
        }
    }

    init {
        listenAdFullScreenEvents()
        observeNativeAds()
    }
    private fun initialize() {
        viewModelScope.launch {
            isFinishingOnboarding = false
            setOnboardingCompleted(false)
        }
    }

    private fun clickNext(page: Int) {
        viewModelScope.launch {
            val nextPage = page + 1
            setState {
                copy(currentPage = nextPage)
            }
            if (nextPage == currentState.totalPage) {
                isFinishingOnboarding = true
                setOnboardingCompleted(true)
                setEffect { OnboardingEffect.ShowFullScreenAd }
            }
        }
    }

    private fun preLoadAds(activity: Activity) {
        viewModelScope.launch {
            //launch { fetchAndActiveConfigsUseCase() }
            adManager.preloadFullScreenAd(activity, AdPlaceName.PASS_INTRO)
            adManager.preloadBannerNativeAd(AdPlaceName.INTRO_V2_BOTTOM)
            val nativeHolder = adManager.getNativeAdHolder(AdPlaceName.INTRO_V2_BOTTOM)
            setState { copy(nativeHolder = nativeHolder, adLoadTrigger = adLoadTrigger + 1 ) }
        }
    }

    private fun observeNativeAds() {
        viewModelScope.launch {
            adManager.observeBannerNativeAd(AdPlaceName.INTRO_V2_BOTTOM).collect {
                val nativeHolder = adManager.getNativeAdHolder(AdPlaceName.INTRO_V2_BOTTOM)
                setState { copy(nativeHolder = nativeHolder, adLoadTrigger = adLoadTrigger + 1 ) }
            }
        }
    }

    private fun showInterAd(activity: Activity) {
        viewModelScope.launch {
            adManager.showFullScreenAd(activity, AdPlaceName.PASS_INTRO)
        }
    }

    private fun listenAdFullScreenEvents() {
        viewModelScope.launch {
            adManager.observeFullScreenAd(AdPlaceName.PASS_INTRO).collect { uiResource ->
                when (uiResource) {
                    is AdFullScreenUiResource.AdLoaded -> {
                        if (isFinishingOnboarding) {
                            setEffect { OnboardingEffect.ShowFullScreenAd }
                        }
                    }
                    is AdFullScreenUiResource.AdCompleted,
                    is AdFullScreenUiResource.AdNotValidOrLoadFailed,
                    is AdFullScreenUiResource.AdFailedToShow -> {
                        if (isFinishingOnboarding) {
                            setEffect { OnboardingEffect.NavigateToHome }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}