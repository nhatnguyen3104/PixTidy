package com.androidtech.domain.usecase.settings

import com.androidtech.domain.model.settings.AppTheme
import com.androidtech.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdateThemeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(theme: AppTheme) = settingsRepository.updateTheme(theme)
}
