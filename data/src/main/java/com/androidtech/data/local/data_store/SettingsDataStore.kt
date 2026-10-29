package com.androidtech.data.local.data_store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.androidtech.domain.model.settings.AppLanguage
import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.domain.model.settings.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * Thin, testable wrapper around a `DataStore<Preferences>` instance. Both keys store an enum's
 * `.name` (not [AppLanguage.languageTag], since [AppTheme] has no natural string code and using
 * `.name` consistently for both keeps the read/parse logic identical for each).
 */
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val userSettings: Flow<UserSettings> = dataStore.data
        .catch { error ->
            // DataStore surfaces a corrupted file as an IOException on read; degrade to defaults
            // rather than crashing the Settings screen.
            if (error is IOException) emit(emptyPreferences())
            else throw error
        }
        .map { preferences ->
            UserSettings(
                theme = preferences[KEY_APP_THEME]?.toAppThemeOrNull() ?: UserSettings.DEFAULT.theme,
                language = preferences[KEY_APP_LANGUAGE]?.toAppLanguageOrNull() ?: UserSettings.DEFAULT.language,
                isOnboardingCompleted = preferences[KEY_ONBOARDING_COMPLETED]?.toOnboardingCompleted() ?: UserSettings.DEFAULT.isOnboardingCompleted //false
            )
        }

    suspend fun updateTheme(theme: AppTheme) {
        dataStore.edit { preferences -> preferences[KEY_APP_THEME] = theme.name }
    }

    suspend fun updateLanguage(language: AppLanguage) {
        dataStore.edit { preferences -> preferences[KEY_APP_LANGUAGE] = language.name }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences -> preferences[KEY_ONBOARDING_COMPLETED] = completed }
    }

    /** Falls back to null (not a thrown exception) for a value from a newer app version we don't recognize. */
    private fun String.toAppThemeOrNull(): AppTheme? = runCatching { AppTheme.valueOf(this) }.getOrNull()

    private fun String.toAppLanguageOrNull(): AppLanguage? = runCatching { AppLanguage.valueOf(this) }.getOrNull()

    private fun Boolean.toOnboardingCompleted(): Boolean = this

    companion object {
        const val PREFERENCES_NAME = "snapsweep_settings"
        val KEY_APP_THEME = stringPreferencesKey("app_theme")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
