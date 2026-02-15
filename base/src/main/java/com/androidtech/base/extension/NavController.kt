package com.androidtech.base.extension

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.NavHostController

fun NavHostController.navigateToRoute(screenName: String) {
    navigate(route = screenName) {
        graph.startDestinationRoute?.let { screen ->
            popUpTo(screen) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

fun NavController.navigateToRoute(screenName: String) {
    navigate(route = screenName) {
        graph.startDestinationRoute?.let { screen ->
            popUpTo(screen) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

val LocalNavController = staticCompositionLocalOf<NavController> { error("No NavController provided") }