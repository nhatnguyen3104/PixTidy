package com.androidtech.features.ui.screens.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.permission.MediaPermissionStatus
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.model.settings.AppLanguage
import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.domain.model.settings.UserSettings
import com.androidtech.domain.usecase.settings.GetUserSettingsUseCase
import com.androidtech.domain.usecase.settings.UpdateLanguageUseCase
import com.androidtech.domain.usecase.settings.UpdateThemeUseCase
import com.androidtech.features.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateThemeUseCase: UpdateThemeUseCase,
    private val updateLanguageUseCase: UpdateLanguageUseCase
): BaseViewModel<SettingsEvent, SettingsState, SettingsEffect>(savedStateHandle) {
    override fun createInitializeState(): SettingsState = SettingsState()

    init {
        observeUserSettings()
    }

    private fun observeUserSettings() {
        getUserSettingsUseCase()
            .onEach { settings ->
                setState { copy(theme = settings.theme, language = settings.language) }
            }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.NavigateBackClicked -> {
                setEffect { SettingsEffect.NavigateBack }
            }
            is SettingsEvent.PrivacyPolicyClicked -> {
                viewModelScope.launch { setEffect { SettingsEffect.OpenWebUrl(BuildConfig.PRIVACY_URL) } }
            }
            is SettingsEvent.TermsOfServiceClicked -> {
                viewModelScope.launch { setEffect { SettingsEffect.OpenWebUrl(BuildConfig.TERMS_URL) } }
            }
            is SettingsEvent.ManagePermissionsClick -> {
                when (uiState.value.permissionStatus) {
                    MediaPermissionStatus.FULL_ACCESS -> {
                        // Already full access - do nothing or show message
                    }
                    MediaPermissionStatus.PARTIAL_ACCESS, MediaPermissionStatus.DENIED -> {
                        // Show educational rationale dialog before requesting
                        setState { copy(showRationaleDialog = true) }
                    }
                    MediaPermissionStatus.PERMANENTLY_DENIED -> {
                        // Show settings redirect dialog
                        setState { copy(showSettingsRedirectDialog = true) }
                    }
                }
            }
            is SettingsEvent.DismissRationaleDialog -> {
                setState { copy(showRationaleDialog = false) }
            }
            is SettingsEvent.ConfirmRationaleDialog -> {
                setState { copy(showRationaleDialog = false) }
                setEffect { SettingsEffect.RequestPermissions }
            }
            is SettingsEvent.DismissSettingsRedirectDialog -> {
                setState { copy(showSettingsRedirectDialog = false) }
            }
            is SettingsEvent.ConfirmSettingsRedirectDialog -> {
                setState { copy(showSettingsRedirectDialog = false) }
                setEffect { SettingsEffect.OpenAppPermissions }
            }
            is SettingsEvent.UpdatePermissionStatus -> {
                setState { copy(permissionStatus = event.status) }
            }
            is SettingsEvent.FeedbackClicked -> {
                // Handle feedback click
            }
            is SettingsEvent.ToggleDarkMode -> {
                val theme = if (event.enabled) AppTheme.DARK else AppTheme.LIGHT
                onThemeSelected(theme)
            }
            is SettingsEvent.LanguageSelected -> {
                onLanguageSelected(event.language)
            }
            is SettingsEvent.CheckForUpdatesClicked -> {
                setEffect { SettingsEffect.CheckForUpdates }
            }
        }
    }

    val userSettings = getUserSettingsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UserSettings.DEFAULT
    )
    fun onThemeSelected(theme: AppTheme) {
        viewModelScope.launch { updateThemeUseCase(theme) }
    }

    fun onLanguageSelected(language: AppLanguage) {
        viewModelScope.launch { updateLanguageUseCase(language) }
    }
}
