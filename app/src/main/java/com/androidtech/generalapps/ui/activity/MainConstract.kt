package com.androidtech.generalapps.ui.activity

import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.settings.AppLanguage
import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.domain.model.settings.UserSettings

sealed class MainEvent(): UIEvent {
    data object LoadRemoteConfig: MainEvent()
    data object LoadUserSettings: MainEvent()
}

data class MainState(
    val testData: String = "",
    val isSettingsReady: Boolean = false,
    val theme: AppTheme = UserSettings.DEFAULT.theme,
    val language: AppLanguage = UserSettings.DEFAULT.language,
    val isOnboardingCompleted: Boolean = false
): UIState

sealed class MainEffect: UIEffect {
    data object TestEffect: MainEffect()
}