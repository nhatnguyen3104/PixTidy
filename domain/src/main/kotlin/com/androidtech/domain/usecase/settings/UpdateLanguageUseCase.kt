package com.androidtech.domain.usecase.settings

import com.androidtech.domain.repository.LocaleController
import com.androidtech.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Persists the chosen language code AND applies it as the live process locale.
 */
class UpdateLanguageUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localeController: LocaleController
) {
    suspend operator fun invoke(languageCode: String) {
        settingsRepository.updateLanguage(languageCode)
        localeController.applyLocale(languageCode)
    }
}
