package com.androidtech.features.ui.screens.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.model.LanguageModel
import com.androidtech.base.permission.MediaPermissionStatus
import com.androidtech.base.viewmodel.BaseViewModel
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
                setState { copy(theme = settings.theme, languageCode = settings.languageCode) }
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
                    }
                    MediaPermissionStatus.PARTIAL_ACCESS, MediaPermissionStatus.DENIED -> {
                        setState { copy(showRationaleDialog = true) }
                    }
                    MediaPermissionStatus.PERMANENTLY_DENIED -> {
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
            }
            is SettingsEvent.ToggleDarkMode -> {
                val theme = if (event.enabled) AppTheme.DARK else AppTheme.LIGHT
                onThemeSelected(theme)
            }
            is SettingsEvent.LanguageSelected -> {
                onLanguageSelected(event.languageModel)
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

    fun onLanguageSelected(languageModel: LanguageModel) {
        viewModelScope.launch { updateLanguageUseCase(languageModel.languageCode) }
    }
}
