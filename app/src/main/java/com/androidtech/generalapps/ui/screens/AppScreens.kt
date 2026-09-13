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
import com.androidtech.features.ui.screens.scan.scanScreen
import com.androidtech.features.ui.screens.similar.similarScreen

@Composable
fun AppScreens(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    CompositionLocalProvider(LocalNavController provides navController) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NavHost(
                navController = navController,
                startDestination = AppDestination.HomeScreen.route
            ) {
                homeScreen()
                scanScreen()
                duplicateScreen()
                similarScreen()
            }
        }
    }
}