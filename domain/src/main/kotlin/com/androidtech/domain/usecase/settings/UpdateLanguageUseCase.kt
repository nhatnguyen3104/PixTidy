package com.androidtech.domain.usecase.settings

import com.androidtech.domain.model.settings.AppLanguage
import com.androidtech.domain.repository.LocaleController
import com.androidtech.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Persists the chosen language AND applies it as the live process locale — the two steps the spec
 * calls out explicitly ("Invokes repository and triggers language locale change globally"). Order
 * matters only cosmetically here (both are idempotent and independent), but persisting first means
 * a process death between the two steps still leaves the correct choice recorded for next launch.
 */
class UpdateLanguageUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localeController: LocaleController
) {
    suspend operator fun invoke(language: AppLanguage) {
        settingsRepository.updateLanguage(language)
        localeController.applyLocale(language)
    }
}
