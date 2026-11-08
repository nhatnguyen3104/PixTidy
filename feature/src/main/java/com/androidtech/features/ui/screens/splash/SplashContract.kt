package com.androidtech.features.ui.screens.splash

import android.app.Activity
import androidx.compose.runtime.Immutable
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState

@Immutable
data class SplashState(
    val progress: Float = 0f,
    val isOnboardingCompleted: Boolean = false,
): UIState

sealed class SplashEvent: UIEvent {
    object StartInitialization : SplashEvent()
    data class PreloadInterAd(val activity: Activity): SplashEvent()
    data class ShowInterAd(val activity: Activity): SplashEvent()
}

sealed class SplashEffect : UIEffect {
    object NavigateToHome : SplashEffect()
    object NavigateToLanguage : SplashEffect()
    object ShowFullScreenAd: SplashEffect()
}