package com.androidtech.generalapps.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ads.monetization.AdManager
import com.androidtech.base.helper.AppUpdateChecker
import com.androidtech.base.theme.AppTheme
import com.androidtech.domain.usecase.settings.GetUserSettingsUseCase
import com.androidtech.generalapps.ui.screens.AppScreens
import com.androidtech.generalapps.ui.theme.GeneralAppsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Anti-flicker technique for the *theme* (language doesn't need this — see
 * [com.androidtech.data.utils.LocaleUtils]'s KDoc for why AppCompatDelegate already
 * persists/applies locale independently of our own DataStore read completing):
 *
 * [SettingsViewModel.uiState] starts at [com.androidtech.domain.model.settings.UserSettings.DEFAULT] and only
 * reflects the real persisted theme once the first DataStore read completes — a few milliseconds,
 * but enough to paint one frame in the wrong theme on a cold start if `setContent` renders
 * immediately. `installSplashScreen().setKeepOnScreenCondition` holds the system splash screen
 * (not a homemade one — no extra Activity, no flicker of its own) until that first *real* read
 * completes, so the very first Compose frame the user sees is already in their chosen theme.
 *
 * Deliberately waits on [GetUserSettingsUseCase] directly (via its own `.first()`), not on
 * [SettingsViewModel.uiState]'s current value — the StateFlow's seeded default is
 * indistinguishable from a genuinely-SYSTEM-themed user's real value, so gating on "has the flow
 * emitted at all yet" (this file) is correct where gating on "does the value still equal
 * UserSettings.DEFAULT" would not be.
 *
 * No corresponding `attachBaseContext` override is needed for locale: `AppCompatDelegate` handles
 * wrapping the base Context internally and recreates the Activity itself when needed, via the
 * platform's own mechanism on API 33+ and via appcompat's activity-recreation hook below it.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel by viewModels<MainViewModel>()

    @Inject
    lateinit var getUserSettingsUseCase: GetUserSettingsUseCase

    private val appUpdateChecker by lazy { AppUpdateChecker(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        appUpdateChecker.check()

        /*splashScreen.setKeepOnScreenCondition { !mainViewModel.uiState.value.isSettingsReady }*/
        setContent {
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            AppTheme(theme = uiState.theme) {
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