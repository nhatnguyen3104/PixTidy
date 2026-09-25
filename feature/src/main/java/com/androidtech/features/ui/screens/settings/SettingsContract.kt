package com.androidtech.features.ui.screens.settings

import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState

data class SettingsState(
    val isLoading: Boolean = false,
    val isDarkMode: Boolean = false,
    val appVersion: String = "1.0.0"
) : UIState

sealed class SettingsEvent : UIEvent {
    object NavigateBackClicked: SettingsEvent()
    object PrivacyPolicyClicked: SettingsEvent()
    object ManagePermissionsClick: SettingsEvent()
    object FeedbackClicked: SettingsEvent()
    data class ToggleDarkMode(val enabled: Boolean) : SettingsEvent()
}

sealed class SettingsEffect : UIEffect {
    object NavigateBack: SettingsEffect()
    data class ShowSnackbar(val message: UiText) : SettingsEffect()
}