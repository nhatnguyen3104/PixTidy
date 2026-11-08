package com.androidtech.domain.repository

import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.domain.model.settings.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * Interface seam for updating live process locale.
 */
interface LocaleController {

    /** Applies [languageCode] as the app's active locale, effective immediately for new Compositions. */
    fun applyLocale(languageCode: String)
}

/** Abstraction over persisted user preferences. Implemented against DataStore in the data layer. */
interface SettingsRepository {

    /** Hot-observable current settings; always emits at least [UserSettings.DEFAULT] immediately. */
    val userSettings: Flow<UserSettings>

    /** Persists a change to the "Theme Mode" setting. */
    suspend fun updateTheme(theme: AppTheme)

    /** Persists a change to the "Language" setting by language code. */
    suspend fun updateLanguage(languageCode: String)

    /** Persists a change to whether onboarding has been completed. */
    suspend fun setOnboardingCompleted(completed: Boolean)
}
