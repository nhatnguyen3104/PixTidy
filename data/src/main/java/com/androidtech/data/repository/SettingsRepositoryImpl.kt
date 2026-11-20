package com.androidtech.data.repository

import com.androidtech.data.local.data_store.SettingsDataStore
import com.androidtech.data.utils.LocaleUtils
import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.domain.model.settings.UserSettings
import com.androidtech.domain.repository.LocaleController
import com.androidtech.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AndroidLocaleController @Inject constructor() : LocaleController {

    override fun applyLocale(languageCode: String) {
        LocaleUtils.applyLocale(languageCode)
    }
}

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val userSettings: Flow<UserSettings> = settingsDataStore.userSettings

    override suspend fun updateTheme(theme: AppTheme) = settingsDataStore.updateTheme(theme)

    override suspend fun updateLanguage(languageCode: String) = settingsDataStore.updateLanguage(languageCode)

    override suspend fun setOnboardingCompleted(completed: Boolean) = settingsDataStore.setOnboardingCompleted(completed)

    override suspend fun setScanRecommended(recommended: Boolean) = settingsDataStore.setScanRecommended(recommended)
}
