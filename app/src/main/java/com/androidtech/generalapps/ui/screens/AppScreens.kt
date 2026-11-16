package com.androidtech.generalapps.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.androidtech.base.extension.AppDestination
import com.androidtech.base.extension.LocalNavController
import com.androidtech.features.ui.screens.duplicate.duplicateScreen
import com.androidtech.features.ui.screens.home.homeScreen
import com.androidtech.features.ui.screens.language.languageScreen
import com.androidtech.features.ui.screens.largevideo.largeVideoScreen
import com.androidtech.features.ui.screens.onboarding.onboardingScreen
import com.androidtech.features.ui.screens.scan.scanScreen
import com.androidtech.features.ui.screens.screenshot.screenshotScreen
import com.androidtech.features.ui.screens.settings.settingsScreen
import com.androidtech.features.ui.screens.similar.similarScreen
import com.androidtech.features.ui.screens.splash.splashScreen
import com.androidtech.features.ui.screens.trash.trashScreen

@Composable
fun AppScreens(
    modifier: Modifier = Modifier,
    onCheckForUpdates: () -> Unit = {}
) {
    val navController = rememberNavController()

    CompositionLocalProvider(LocalNavController provides navController) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.SplashScreen.route
            ) {
                splashScreen()
                languageScreen()
                onboardingScreen()
                homeScreen()
                trashScreen()
                scanScreen()
                duplicateScreen()
                similarScreen()
                screenshotScreen()
                largeVideoScreen()
                settingsScreen(onCheckForUpdates = onCheckForUpdates)
            }
        }
    }
}