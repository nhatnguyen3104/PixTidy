package com.androidtech.features.ui.screens.onboarding

import androidx.compose.runtime.Immutable
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState

@Immutable
data class OnboardingState(
    val currentPage: Int = 0,
    val isPrivacyAccepted: Boolean = false
): UIState

sealed class OnboardingEvent : UIEvent {
    data class PageChanged(val page: Int) : OnboardingEvent()
    object NextClicked : OnboardingEvent()
    data class TogglePrivacyAcceptance(val accepted: Boolean) : OnboardingEvent()
    object TermsOfServiceClicked : OnboardingEvent()
    object PrivacyPolicyClicked : OnboardingEvent()
}

sealed class OnboardingEffect : UIEffect {
    data class ScrollToPage(val page: Int) : OnboardingEffect()
    object NavigateToHome : OnboardingEffect()
    data class OpenWebUrl(val url: String) : OnboardingEffect()
}