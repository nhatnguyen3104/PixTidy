package com.androidtech.base.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.androidtech.domain.model.settings.AppTheme

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = BrandPrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,

    secondary = SecondaryLight,
    tertiary = WarningYellow,

    background = BackgroundLight,
    onBackground = OnBackgroundLight,

    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,

    error = ErrorRed,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,

    outline = OutlineDark,
    tertiary = WarningYellow,
    secondary = SecondaryDark
)


/**
 * Top-level theme wrapper. [theme] is expected to come from
 * `SettingsViewModel.uiState.collectAsStateWithLifecycle().value.theme` at the composition root
 * (see the sample wiring in this file's KDoc) — this composable itself has no ViewModel
 * dependency, so it stays trivially `@Preview`-able.
 *
 * `isSystemInDarkTheme()` is called unconditionally (not just inside the `SYSTEM` branch) because
 * Compose requires the same sequence of composable calls across recompositions; calling it only
 * conditionally would violate that rule the moment the user switches away from `SYSTEM`.
 */
@Composable
fun AppTheme(
    theme: AppTheme,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = when (theme) {
        AppTheme.SYSTEM -> systemInDarkTheme
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
