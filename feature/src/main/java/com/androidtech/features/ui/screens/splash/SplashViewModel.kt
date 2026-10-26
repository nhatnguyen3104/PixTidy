package com.androidtech.features.ui.screens.splash

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.usecase.settings.GetUserSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class SplashViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUserSettingsUseCase: GetUserSettingsUseCase
): BaseViewModel<SplashEvent, SplashState, SplashEffect>(savedStateHandle) {
    override fun createInitializeState(): SplashState = SplashState()

    override fun handleEvent(event: SplashEvent) {
        when(event) {
            is SplashEvent.StartInitialization -> {
                startInitialization()
            }
        }
    }

    private fun startInitialization() {

        /** Preload ads if wish
         * adManager.preloadAd(context, AdType.NATIVE)
         * adManager.preloadAd(context, AdType.BANNER)
         * adManager.preloadAd(context, AdType.INTERSTITIAL)
         */

        viewModelScope.launch {
            val totalSteps = 100
            for (i in 1..totalSteps) {
                delay(10.milliseconds) // Reduced delay for faster dev testing if needed
                setState { copy(progress = i.toFloat() / totalSteps) }
            }

            val settings = getUserSettingsUseCase().first()
            setState { copy(isOnboardingCompleted = settings.isOnboardingCompleted) }
            if (settings.isOnboardingCompleted) {
                setEffect { SplashEffect.NavigateToHome }
            } else {
                setEffect { SplashEffect.NavigateToOnboarding }
            }
        }
    }
}