package com.androidtech.domain.repository

import com.androidtech.domain.model.settings.AppLanguage
import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.domain.model.settings.UserSettings
import kotlinx.coroutines.flow.Flow

/**
 * ADDITION beyond the literal spec: section 4.3 requires `UpdateLanguageUseCase` to both persist
 * the choice AND "trigger language locale change globally" — but actually changing the process
 * locale means calling an AndroidX API (`AppCompatDelegate.setApplicationLocales`), which the pure-
 * Kotlin domain layer cannot reference directly. This interface is the seam: the use case depends
 * on this abstraction, and the data layer's `AndroidLocaleController` is the concrete
 * AppCompatDelegate-backed implementation. Domain stays framework-free; the actual mutation happens
 * in exactly one place.
 */
interface LocaleController {

    /** Applies [language] as the app's active locale, effective immediately for new Compositions. */
    fun applyLocale(language: AppLanguage)
}

/** Abstraction over persisted user preferences. Implemented against DataStore in the data layer. */
interface SettingsRepository {

    /** Hot-observable current settings; always emits at least [UserSettings.DEFAULT] immediately. */
    val userSettings: Flow<UserSettings>

    /** Persists a change to the "Theme Mode" setting. */
    suspend fun updateTheme(theme: AppTheme)

    /** Persists a change to the "Language" setting. */
    suspend fun updateLanguage(language: AppLanguage)

    /** Persists a change to whether onboarding has been completed. */
    suspend fun setOnboardingCompleted(completed: Boolean)
}