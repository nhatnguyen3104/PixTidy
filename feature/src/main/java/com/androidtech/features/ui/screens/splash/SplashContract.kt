package com.androidtech.features.ui.screens.splash

import android.app.Activity
import androidx.compose.runtime.Immutable
import com.ads.monetization.adholder.NativeAdHolder
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState

@Immutable
data class SplashState(
    val progress: Float = 0f,
    val isOnboardingCompleted: Boolean = false,
    val nativeHolder: NativeAdHolder? = null,
    val adLoadTrigger: Int = 0
): UIState

sealed class SplashEvent: UIEvent {
    data object StartInitialization : SplashEvent()
    data class PreloadAds(val activity: Activity): SplashEvent()
    data class ShowInterAd(val activity: Activity): SplashEvent()
}

sealed class SplashEffect : UIEffect {
    data object NavigateToHome : SplashEffect()
    data object NavigateToLanguage : SplashEffect()
    data object ShowFullScreenAd: SplashEffect()
}