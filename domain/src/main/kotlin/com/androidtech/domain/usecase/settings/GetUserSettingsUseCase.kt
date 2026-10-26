package com.androidtech.domain.usecase.settings

import com.androidtech.domain.model.settings.UserSettings
import com.androidtech.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetUserSettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<UserSettings> = settingsRepository.userSettings
}