package com.androidtech.domain.model.onboarding

sealed interface OnboardingPage {
    val page: Int

    data object FirstOnboarding: OnboardingPage {
        override val page: Int
            get() = 0
    }

    data object SecondOnboarding: OnboardingPage {
        override val page: Int
            get() = 1
    }

    data object ThirdOnboarding: OnboardingPage {
        override val page: Int
            get() = 2
    }
}