package com.androidtech.features.ui.screens.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.usecase.settings.SetOnboardingCompleted
import com.androidtech.features.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val setOnboardingCompleted: SetOnboardingCompleted
): BaseViewModel<OnboardingEvent,OnboardingState, OnboardingEffect>(savedStateHandle) {
    override fun createInitializeState(): OnboardingState = OnboardingState()

    override fun handleEvent(event: OnboardingEvent) {
        when(event) {
            is OnboardingEvent.PageChanged -> {
                setState { copy(currentPage = event.page) }
            }
            is OnboardingEvent.NextClicked -> {
                val current = uiState.value.currentPage
                if (current < 2) {
                    viewModelScope.launch { setEffect{OnboardingEffect.ScrollToPage(current + 1)} }
                } else if (uiState.value.isPrivacyAccepted) {
                    completeOnboarding()
                }
            }
            is OnboardingEvent.TogglePrivacyAcceptance -> {
                setState {
                    copy(isPrivacyAccepted = event.accepted)
                }
            }
            is OnboardingEvent.TermsOfServiceClicked -> {
                viewModelScope.launch { setEffect { OnboardingEffect.OpenWebUrl("https://snapsweep.app/terms") } }
            }
            is OnboardingEvent.PrivacyPolicyClicked -> {
                viewModelScope.launch { setEffect { OnboardingEffect.OpenWebUrl("https://snapsweep.app/privacy") } }
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            setOnboardingCompleted(true)
            setEffect{ OnboardingEffect.NavigateToHome}
        }
    }
}