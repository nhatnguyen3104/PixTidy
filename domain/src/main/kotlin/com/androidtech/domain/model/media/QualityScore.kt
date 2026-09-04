package com.androidtech.domain.model.media

/**
 * Result of a sharpness/exposure evaluation for a single image.
 *
 * @param score 0 (worst) .. 100 (best) composite quality score.
 */
data class QualityScore(
    val score: Int,
    val isBlurry: Boolean,
    val isOverexposed: Boolean,
    val isUnderexposed: Boolean
) {
    init {
        require(score in 0..100) { "QualityScore.score must be within 0..100, was $score" }
    }

    companion object {
        /** Neutral fallback used when analysis could not be performed (e.g. decode failure). */
        val UNKNOWN = QualityScore(score = 50, isBlurry = false, isOverexposed = false, isUnderexposed = false)
    }
}
