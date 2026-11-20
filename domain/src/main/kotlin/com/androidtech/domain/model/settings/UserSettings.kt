package com.androidtech.domain.model.settings

data class UserSettings(
    val theme: AppTheme,
    val languageCode: String,
    val isOnboardingCompleted: Boolean,
    val isScanRecommended: Boolean = true
) {
    companion object {
        val DEFAULT = UserSettings(
            theme = AppTheme.LIGHT,
            languageCode = "",
            isOnboardingCompleted = false,
            isScanRecommended = true
        )
    }
}
