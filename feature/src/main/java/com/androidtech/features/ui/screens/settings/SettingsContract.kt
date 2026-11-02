package com.androidtech.features.ui.screens.settings

import com.androidtech.base.permission.MediaPermissionStatus
import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.settings.AppLanguage
import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.domain.model.settings.UserSettings

data class SettingsState(
    val isLoading: Boolean = false,
    val theme: AppTheme = UserSettings.DEFAULT.theme,
    val language: AppLanguage = UserSettings.DEFAULT.language,
    val appVersion: String = "0.0.1",
    val permissionStatus: MediaPermissionStatus = MediaPermissionStatus.DENIED,
    val showRationaleDialog: Boolean = false,
    val showSettingsRedirectDialog: Boolean = false
) : UIState

sealed class SettingsEvent : UIEvent {
    object NavigateBackClicked: SettingsEvent()
    object PrivacyPolicyClicked: SettingsEvent()
    object TermsOfServiceClicked: SettingsEvent()
    object ManagePermissionsClick: SettingsEvent()
    object DismissRationaleDialog: SettingsEvent()
    object ConfirmRationaleDialog: SettingsEvent()
    object DismissSettingsRedirectDialog: SettingsEvent()
    object ConfirmSettingsRedirectDialog: SettingsEvent()
    data class UpdatePermissionStatus(val status: MediaPermissionStatus) : SettingsEvent()
    object FeedbackClicked: SettingsEvent()
    data class ToggleDarkMode(val enabled: Boolean) : SettingsEvent()
    data class LanguageSelected(val language: AppLanguage) : SettingsEvent()
}

sealed class SettingsEffect : UIEffect {
    object NavigateBack: SettingsEffect()
    data class ShowSnackbar(val message: UiText) : SettingsEffect()
    data class OpenWebUrl(val url: String) : SettingsEffect()
    object OpenAppPermissions : SettingsEffect()
    object RequestPermissions : SettingsEffect()
}
