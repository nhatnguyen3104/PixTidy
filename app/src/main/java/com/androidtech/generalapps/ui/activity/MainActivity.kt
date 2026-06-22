package com.androidtech.generalapps.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.ads.monetization.AdManager
import com.androidtech.generalapps.ui.screens.AppScreens
import com.androidtech.generalapps.ui.theme.GeneralAppsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GeneralAppsTheme {
                mainViewModel.setEvent(MainEvent.LoadRemoteConfig)
                Scaffold(modifier = Modifier.Companion.fillMaxSize()) { innerPadding ->
                    AppScreens(
                        modifier = Modifier.Companion.fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}