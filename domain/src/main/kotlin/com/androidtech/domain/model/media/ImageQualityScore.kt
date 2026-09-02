package com.androidtech.domain.model.media

/**
 * Quality metrics calculated from a downscaled bitmap.
 *
 * [blurrinessScore] is normalized from 0 to 100 where a higher
 * value generally indicates greater edge detail.
 */
data class ImageQualityScore(
    val overallScore: Int,
    val blurrinessScore: Float,
    val isBlurry: Boolean,
    val isDark: Boolean
)