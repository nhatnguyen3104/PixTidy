package com.androidtech.domain.usecase.settings

import com.androidtech.domain.repository.SettingsRepository
import javax.inject.Inject

class SetOnboardingCompleted @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(completed: Boolean) = settingsRepository.setOnboardingCompleted(completed)
}