package com.androidtech.features.ui.screens.language

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ads.monetization.AdManager
import com.ads.monetization.adplacename.AdPlaceName
import com.ads.monetization.adholder.NativeAdHolder
import com.androidtech.data.model.aduiresource.AdLoadBannerNativeUiResource
import com.androidtech.data.model.aduiresource.getAdPlaceName
import com.androidtech.base.model.LanguageModel
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.usecase.FetchAndActiveRemoteConfigsUseCase
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
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateLanguageUseCase: UpdateLanguageUseCase,
    private val adManager: AdManager,
    private val fetchAndActiveConfigsUseCase: FetchAndActiveRemoteConfigsUseCase,
) : BaseViewModel<LanguageEvent, LanguageState, LanguageEffect>(savedStateHandle) {
    override fun createInitializeState(): LanguageState = LanguageState()

    override fun handleEvent(event: LanguageEvent) {
        when (event) {
            is LanguageEvent.ClickLanguageItem -> clickLanguage(event.languageModel)
            is LanguageEvent.ClickToSaveLanguage -> saveLanguageAndNavigateToHome()
            is LanguageEvent.PreloadAds -> preloadAds()
        }
    }

    init {
        initializeCurrentLanguage()
        preloadAds()
        observeAds()
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
            val isOnboardingCompleted = getUserSettingsUseCase().first().isOnboardingCompleted
            setState { copy(isShowLoading = true) }
            updateLanguageUseCase(selectedCode)

            delay(1500.milliseconds)
            setState { copy(isShowLoading = false) }
            delay(100.milliseconds)
            if (!isOnboardingCompleted) {
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

    private fun preloadAds() {
        viewModelScope.launch {
            launch { fetchAndActiveConfigsUseCase() }
            adManager.preloadBannerNativeAd(placeName = AdPlaceName.LANGUAGE_BOTTOM)
            val nativeHolder = adManager.getNativeAdHolder(AdPlaceName.LANGUAGE_BOTTOM)
            setState { copy(nativeHolder = nativeHolder, adLoadTrigger = adLoadTrigger + 1) }
        }
    }

    private fun observeAds() {
        viewModelScope.launch {
            adManager.observeBannerNativeAd(AdPlaceName.LANGUAGE_BOTTOM).collect {
                val nativeHolder = adManager.getNativeAdHolder(AdPlaceName.LANGUAGE_BOTTOM)
                setState { copy(nativeHolder = nativeHolder, adLoadTrigger = adLoadTrigger + 1) }
            }
        }
    }
}
