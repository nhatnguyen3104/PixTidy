package com.androidtech.features.ui.screens.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.model.settings.AppLanguage
import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.domain.model.settings.UserSettings
import com.androidtech.domain.usecase.settings.GetUserSettingsUseCase
import com.androidtech.domain.usecase.settings.UpdateLanguageUseCase
import com.androidtech.domain.usecase.settings.UpdateThemeUseCase
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
                // Handle privacy policy click
            }
            is SettingsEvent.ManagePermissionsClick -> {
                // Handle permissions click
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
        }
    }

    val userSettings = getUserSettingsUseCase().stateIn(
        scope = viewModelScope,
        // 5s grace period so a config change (e.g. the locale-triggered Activity recreation
        // this screen itself can cause) doesn't tear down and immediately restart collection.
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