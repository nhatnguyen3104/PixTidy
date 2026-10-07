package com.androidtech.domain.model.settings

data class UserSettings(
    val theme: AppTheme,
    val language: AppLanguage,
    val isOnboardingCompleted: Boolean,
) {
    companion object {
        val DEFAULT = UserSettings(theme = AppTheme.SYSTEM, language = AppLanguage.SYSTEM, isOnboardingCompleted = false)
    }
}
