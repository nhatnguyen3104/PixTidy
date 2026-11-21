package com.androidtech.domain.model.media

/**
 * Pre-calculated, cacheable subset of a media item's Smart Scan analysis. Deliberately narrower
 * than [QualityScore] — only [blurScore] (the final 0..100 composite) is persisted, not the raw
 * exposure flags, so a cache hit reconstructs [QualityScore] with [QualityScore.isOverexposed] /
 * [QualityScore.isUnderexposed] defaulted to false. That trade-off is what the cache schema calls
 * for; exposure classification is cheap enough relative to decoding that this is an acceptable gap.
 */
data class CachedMediaAnalysis(
    val mediaId: Long,
    val phash: String?,
    val blurScore: Int,
    val isScreenshot: Boolean,
    val videoFrames: List<FrameSample>? = null,
    val isDocument: Boolean? = null,
    val documentConfidence: Float? = null,
    val ocrPreview: String? = null
)
