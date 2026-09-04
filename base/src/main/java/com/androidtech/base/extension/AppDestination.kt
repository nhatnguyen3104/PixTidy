package com.androidtech.base.extension

sealed class AppDestination(val route: String) {
    data object SplashScreen: AppDestination("splash_screen")
    data object HomeScreen: AppDestination("home_screen")
    data object ScanScreen: AppDestination("scan_screen/{mode}") {
        fun createRoute(mode: String) = "scan_screen/$mode"
    }
}
