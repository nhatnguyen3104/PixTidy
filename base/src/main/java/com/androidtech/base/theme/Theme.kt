package com.androidtech.base.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = OnPrimaryLight,
    primaryContainer = BrandPrimaryContainer,

    secondary = AiPurple,
    tertiary = SuccessGreen,

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
    primaryContainer = BrandPrimaryContainerDark,

    background = BackgroundDark,
    onBackground = OnBackgroundDark,

    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,

    outline = OutlineDark,
    tertiary = SuccessGreen,
    secondary = AiPurple
)
