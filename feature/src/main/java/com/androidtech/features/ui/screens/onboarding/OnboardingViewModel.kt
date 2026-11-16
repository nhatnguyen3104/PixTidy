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
            is OnboardingEvent.Initialize -> initialize()
            is OnboardingEvent.ClickNext -> clickNext(event.page)
        }
    }

    private fun initialize() {
        viewModelScope.launch {
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
                setOnboardingCompleted(true)
                setEffect { OnboardingEffect.NavigateToHome }
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