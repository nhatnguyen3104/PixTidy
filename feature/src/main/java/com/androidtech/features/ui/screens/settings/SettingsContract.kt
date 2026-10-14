package com.androidtech.features.ui.screens.settings

import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.settings.AppLanguage
import com.androidtech.domain.model.settings.AppTheme

data class SettingsState(
    val isLoading: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val appVersion: String = "1.0.0"
) : UIState

sealed class SettingsEvent : UIEvent {
    object NavigateBackClicked: SettingsEvent()
    object PrivacyPolicyClicked: SettingsEvent()
    object TermsOfServiceClicked: SettingsEvent()
    object ManagePermissionsClick: SettingsEvent()
    object FeedbackClicked: SettingsEvent()
    data class ToggleDarkMode(val enabled: Boolean) : SettingsEvent()
    data class LanguageSelected(val language: AppLanguage) : SettingsEvent()
}

sealed class SettingsEffect : UIEffect {
    object NavigateBack: SettingsEffect()
    data class ShowSnackbar(val message: UiText) : SettingsEffect()
    data class OpenWebUrl(val url: String) : SettingsEffect()
}