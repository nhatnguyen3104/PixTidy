package com.androidtech.features.ui.screens.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.model.LanguageModel
import com.androidtech.base.prefs.PreferenceHelper
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.usecase.settings.GetUserSettingsUseCase
import com.androidtech.domain.usecase.settings.UpdateLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class LanguageViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val preferenceHelper: PreferenceHelper,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateLanguageUseCase: UpdateLanguageUseCase
) : BaseViewModel<LanguageEvent, LanguageState, LanguageEffect>(savedStateHandle) {
    override fun createInitializeState(): LanguageState = LanguageState()

    override fun handleEvent(event: LanguageEvent) {
        when (event) {
            is LanguageEvent.ClickLanguageItem -> clickLanguage(event.languageModel)
            is LanguageEvent.ClickToSaveLanguage -> saveLanguageAndNavigateToHome()
        }
    }

    init {
        initializeCurrentLanguage()
    }

    private fun clickLanguage(selectedLanguageModel: LanguageModel) {
        viewModelScope.launch {
            setState {
                val newAllLanguages = allLanguages.map { languageModel ->
                    languageModel.copy(isSelected = selectedLanguageModel.languageCode == languageModel.languageCode)
                }

                copy(
                    allLanguages = newAllLanguages,
                    saveLanguageModel = selectedLanguageModel
                )
            }
        }
    }

    private fun saveLanguageAndNavigateToHome() {
        viewModelScope.launch {
            val selectedCode = currentState.saveLanguageModel.languageCode
            updateLanguageUseCase(selectedCode)

            delay(100.milliseconds)
            setState { copy(isShowLoading = true) }
            delay(2000.milliseconds)
            setState { copy(isShowLoading = false) }
            delay(100.milliseconds)
            if (preferenceHelper.isFirstOpen) {
                setEffect { LanguageEffect.NavigateToOnboarding }
            } else {
                setEffect { LanguageEffect.NavigateToHome }
            }
        }
    }

    private fun initializeCurrentLanguage() {
        viewModelScope.launch {
            val settings = getUserSettingsUseCase().first()
            val currentLanguageCode = settings.languageCode.ifEmpty {
                val appLocales = AppCompatDelegate.getApplicationLocales()
                if (!appLocales.isEmpty) {
                    appLocales.toLanguageTags()
                } else {
                    Locale.getDefault().language
                }
            }

            setState {
                val newAllLanguages = allLanguages.map { model ->
                    val isMatch = currentLanguageCode.equals(model.languageCode, ignoreCase = true) ||
                            (model.languageCode.isNotEmpty() && currentLanguageCode.startsWith(model.languageCode, ignoreCase = true))
                    model.copy(isSelected = isMatch)
                }

                val selectedModel = newAllLanguages.find { it.isSelected } ?: LanguageModel.ENGLISH

                copy(
                    allLanguages = newAllLanguages,
                    saveLanguageModel = selectedModel
                )
            }
        }
    }
}
