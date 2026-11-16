package com.androidtech.features.ui.screens.onboarding

import androidx.compose.runtime.Immutable
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState

@Immutable
data class OnboardingState(
    val totalPage: Int = 3,
    val currentPage: Int = 0
): UIState

sealed class OnboardingEvent : UIEvent {
    object Initialize: OnboardingEvent()
    data class ClickNext(val page: Int): OnboardingEvent()
}

sealed class OnboardingEffect : UIEffect {
    data class ScrollToPage(val page: Int) : OnboardingEffect()
    object NavigateToHome : OnboardingEffect()
}