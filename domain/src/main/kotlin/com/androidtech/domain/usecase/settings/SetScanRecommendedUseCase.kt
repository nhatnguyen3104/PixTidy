package com.androidtech.domain.usecase.settings

import com.androidtech.domain.repository.SettingsRepository
import javax.inject.Inject

class SetScanRecommendedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(recommended: Boolean) = settingsRepository.setScanRecommended(recommended)
}
