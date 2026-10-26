package com.androidtech.base.theme

import androidx.compose.ui.graphics.Color

// ===================== BRAND COLORS =====================
// Inspired by camera lens and scanning technology
val BrandPrimaryLight = Color(0xFF0066FF)          // Electric blue - primary brand color
val BrandPrimaryContainerLight = Color(0xFFD6E8FF) // Light blue background

val BrandPrimaryDark = Color(0xFF5B9EFF)           // Brighter blue on dark background
val BrandPrimaryContainerDark = Color(0xFF003399)  // Dark blue for dark container

// ===================== SURFACES =====================
// Neutral tones, minimalistic approach
val BackgroundLight = Color(0xFFF4F7FB)            // Cool-tinted light background
val SurfaceLight = Color(0xFFFFFFFF)                // Pure white
val SurfaceVariantLight = Color(0xFFE8ECF2)         // Light gray

val BackgroundDark = Color(0xFF0A0E17)             // Charcoal blue-black - premium feel
val SurfaceDark = Color(0xFF151C26)                // Blue-tinted dark gray
val SurfaceVariantDark = Color(0xFF1F2A36)          // Dark gray

// ===================== TEXT =====================
val OnPrimaryLight = Color(0xFFFFFFFF)
val OnPrimaryContainerLight = Color(0xFF003399)
val OnBackgroundLight = Color(0xFF111827)
val OnSurfaceLight = Color(0xFF1F2937)

val OnPrimaryDark = Color(0xFF003399)
val OnPrimaryContainerDark = Color(0xFFB3D9FF)
val OnBackgroundDark = Color(0xFFF1F5F9)
val OnSurfaceDark = Color(0xFFE2E8F0)

// ===================== OUTLINE =====================
val OutlineLight = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF475569)

// ===================== SECONDARY =====================
// Secondary color - tech green (inspired by "sweep/scan")
val SecondaryLight = Color(0xFF00C853)             // Neon green
val SecondaryDark = Color(0xFF66FF99)              // Bright green on dark background

// ===================== FUNCTIONAL =====================
val SuccessGreen = Color(0xFF22C55E)               // Success green
val WarningYellow = Color(0xFFFBBF24)              // Warning yellow
val ErrorRed = Color(0xFFEF4444)                   // Error red
val AiPurple = Color(0xFF8B5CF6)                   // AI purple - tech accent

// ===================== CUSTOM UI =====================
val TextTitleColor = Color(0xFF0F172A)             // Dark title
val TextTitleLightColor = Color(0xFFF8FAFC)        // Light title
val TextHintColor = Color(0xFF64748B)              // Hint text
val TextHighlight = Color(0xFF3B82F6)              // Highlight blue
val TextDateTimeColor = Color(0xFF94A3B8)          // Dimmed date/time

val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceBlack = Color(0xFF000000)
val SurfaceHomeItemBackgroundColor = Color(0xFFF1F5F9)
val OutlineColor = Color(0xFFE2E8F0)

// ===================== SNAP SWEEP BADGES =====================
// Distinct colors for file types and labels
val DocumentBadge = Color(0xFF60A5FA)              // Document blue
val ImageBadge = Color(0xFFF472B6)                 // Image pink
val VideoBadge = Color(0xFFA78BFA)                 // Video purple
val AudioBadge = Color(0xFF34D399)                 // Audio green
val FolderBadge = Color(0xFFFBBF24)                // Folder yellow
val ArchiveBadge = Color(0xFFF87171)               // Archive red

// Functional badges
val CameraBadge = Color(0xFF3B82F6)                // Camera blue
val ScanBadge = Color(0xFF8B5CF6)                  // Scan purple
val SweepBadge = Color(0xFF10B981)                 // Sweep/clean green
val StorageBadge = Color(0xFF6B7280)               // Storage gray

// ===================== GRADIENT ACCENTS =====================
// Colors for gradient effects
val GradientStart = Color(0xFF0066FF)
val GradientEnd = Color(0xFF8B5CF6)                // Blue -> Purple
val GradientCameraStart = Color(0xFF00C853)
val GradientCameraEnd = Color(0xFF3B82F6)          // Neon green -> Blue

// ===================== HELPER =====================
fun Color.withAlpha1(alpha: Float = 0.1f): Color {
    return this.copy(alpha = alpha)
}
fun Color.withAlpha2(alpha: Float = 0.2f): Color {
    return this.copy(alpha = alpha)
}
fun Color.withAlpha3(alpha: Float = 0.3f): Color {
    return this.copy(alpha = alpha)
}
fun Color.withAlpha4(alpha: Float = 0.4f): Color {
    return this.copy(alpha = alpha)
}
fun Color.withAlpha5(alpha: Float = 0.5f): Color {
    return this.copy(alpha = alpha)
}
fun Color.withAlpha6(alpha: Float = 0.6f): Color {
    return this.copy(alpha = alpha)
}
fun Color.withAlpha7(alpha: Float = 0.7f): Color {
    return this.copy(alpha = alpha)
}
fun Color.withAlpha8(alpha: Float = 0.8f): Color {
    return this.copy(alpha = alpha)
}
fun Color.withAlpha9(alpha: Float = 0.9f): Color {
    return this.copy(alpha = alpha)
}

// ===================== SHADOW / OVERLAY =====================
val ShadowLight = Color(0x1A000000)                // Light drop shadow (10% alpha)
val ShadowMedium = Color(0x33000000)               // Medium drop shadow (20% alpha)
val ShadowDark = Color(0x66000000)                 // Heavy drop shadow (40% alpha)

val OverlayLight = Color(0x0D000000)               // 5% overlay
val OverlayMedium = Color(0x1A000000)              // 10% overlay
val OverlayHeavy = Color(0x4D000000)               // 30% overlay

// ===================== TRANSPARENT VARIANTS =====================
// Transparent variants for Glassmorphism effects
val GlassBackground = Color(0xCCFFFFFF)            // White glass (80%)
val GlassBackgroundDark = Color(0xCC0A0E17)        // Dark glass (80%)
val GlassBorder = Color(0x33FFFFFF)                // Frosted glass border