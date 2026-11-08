package com.androidtech.domain.model.settings

data class UserSettings(
    val theme: AppTheme,
    val languageCode: String,
    val isOnboardingCompleted: Boolean,
) {
    companion object {
        val DEFAULT = UserSettings(theme = AppTheme.LIGHT, languageCode = "en", isOnboardingCompleted = false)
    }
}
