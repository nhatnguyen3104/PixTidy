package com.androidtech.data.repository

import com.androidtech.data.local.data_store.SettingsDataStore
import com.androidtech.data.utils.LocaleUtils
import com.androidtech.domain.model.settings.AppLanguage
import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.domain.model.settings.UserSettings
import com.androidtech.domain.repository.LocaleController
import com.androidtech.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * DI-testable implementation of the domain [LocaleController] seam, used by
 * [com.androidtech.domain.usecase.settings.UpdateLanguageUseCase]. Delegates to [LocaleUtils] rather than
 * duplicating the AppCompatDelegate call, so there is exactly one place that actually touches the
 * AndroidX locale API — this class exists only so the use case can depend on an interface instead
 * of a static object, keeping it unit-testable with a fake.
 */
class AndroidLocaleController @Inject constructor() : LocaleController {

    override fun applyLocale(language: AppLanguage) {
        LocaleUtils.applyLocale(language)
    }
}


class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : SettingsRepository {

    override val userSettings: Flow<UserSettings> = settingsDataStore.userSettings

    override suspend fun updateTheme(theme: AppTheme) = settingsDataStore.updateTheme(theme)

    override suspend fun updateLanguage(language: AppLanguage) = settingsDataStore.updateLanguage(language)
}
