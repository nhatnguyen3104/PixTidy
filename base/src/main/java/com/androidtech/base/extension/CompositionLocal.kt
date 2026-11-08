package com.androidtech.base.extension

import androidx.compose.runtime.staticCompositionLocalOf
import com.androidtech.base.prefs.PreferenceHelper

val LocalPreferenceHelper = staticCompositionLocalOf<PreferenceHelper> { error("No PreferenceHelper provided") }