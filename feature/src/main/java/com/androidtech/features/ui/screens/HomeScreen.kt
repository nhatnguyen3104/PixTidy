package com.androidtech.features.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.androidtech.base.extension.AppDestination

fun NavGraphBuilder.homeScreen() {
    composable(
        route = AppDestination.HomeScreen.route,

    ) {
        HomeScreen(
            modifier = Modifier.fillMaxSize()
                .background(Color.White)
        )
    }
}

@Composable
@Preview
private fun PreviewHomeScreen() {
    HomeScreen(
        modifier = Modifier.fillMaxSize()
            .background(Color.White)
    )
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            text = "Home Screen",
            color = Color.Black
        )
    }
}