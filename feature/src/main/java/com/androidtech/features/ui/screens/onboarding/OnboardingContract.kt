package com.androidtech.features.ui.screens.onboarding

import android.app.Activity
import androidx.compose.runtime.Immutable
import com.ads.monetization.adholder.NativeAdHolder
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState

@Immutable
data class OnboardingState(
    val totalPage: Int = 3,
    val currentPage: Int = 0,
    val nativeHolder: NativeAdHolder? = null,
): UIState

sealed class OnboardingEvent : UIEvent {
    object Initialize: OnboardingEvent()
    data class ClickNext(val page: Int): OnboardingEvent()
    data class PreloadAds(val activity: Activity): OnboardingEvent()
    data class ShowInterAd(val activity: Activity): OnboardingEvent()
}

sealed class OnboardingEffect : UIEffect {
    data class ScrollToPage(val page: Int) : OnboardingEffect()
    object NavigateToHome : OnboardingEffect()
    object ShowFullScreenAd : OnboardingEffect()
}