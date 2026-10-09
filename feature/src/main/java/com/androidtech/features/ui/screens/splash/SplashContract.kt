package com.androidtech.features.ui.screens.splash

import androidx.compose.runtime.Immutable
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState

@Immutable
data class SplashState(
    val progress: Float = 0f,
): UIState

sealed class SplashEvent: UIEvent {
    object StartInitialization : SplashEvent()
}

sealed class SplashEffect : UIEffect {
    object NavigateToHome : SplashEffect()
    object NavigateToOnboarding : SplashEffect()
}