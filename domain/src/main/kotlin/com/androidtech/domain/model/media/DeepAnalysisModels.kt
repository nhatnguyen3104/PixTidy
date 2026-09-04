package com.androidtech.domain.model.media

/**
 * A single sampled frame from a video, reduced to its perceptual hash only — never a raw Bitmap,
 * to keep this pure-Kotlin domain module free of Android graphics types.
 */
data class FrameSample(
    val timestampMs: Long,
    val phash: String
)

/**
 * Result of OCR-based document/screenshot-of-text detection used by Deep Smart Scan to
 * distinguish "document photos" (receipts, whiteboards, screenshots of text) from regular photos.
 */
data class DocumentAnalysisResult(
    val isLikelyDocument: Boolean,
    val confidence: Float,
    val extractedTextPreview: String?
) {
    init {
        require(confidence in 0f..1f) { "confidence must be within 0.0..1.0, was $confidence" }
    }
}