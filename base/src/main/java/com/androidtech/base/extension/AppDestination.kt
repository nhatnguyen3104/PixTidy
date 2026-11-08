package com.androidtech.base.extension

import kotlinx.serialization.Serializable

@Serializable
sealed class AppDestination(val route: String) {

    @Serializable
    data object SplashScreen: AppDestination("splash_screen")

    @Serializable
    data object LanguageScreen: AppDestination("language_screen")

    @Serializable
    data object OnboardingScreen: AppDestination("onboarding_screen")

    @Serializable
    data object HomeScreen: AppDestination("home_screen")
    @Serializable
    data object ScanScreen: AppDestination("scan_screen/{mode}") {
        fun createRoute(mode: String) = "scan_screen/$mode"
    }
    @Serializable
    data object DuplicateScreen: AppDestination("duplicate_screen")
    @Serializable
    data object SimilarScreen: AppDestination("similar_screen")
    @Serializable
    data object ScreenshotScreen: AppDestination("screenshot_screen")
    @Serializable
    data object SettingsScreen: AppDestination("settings_screen")
}
