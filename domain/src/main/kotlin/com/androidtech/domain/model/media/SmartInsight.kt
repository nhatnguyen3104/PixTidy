package com.androidtech.domain.model.media

enum class InsightType {
    EXACT_DUPLICATE,
    SIMILAR_PHOTOS,
    SCREENSHOTS,
    LARGE_VIDEOS,
    BLURRY_PHOTOS,
    DEEP_OCR_DOCUMENTS
}

/**
 * A single actionable, explainable recommendation surfaced to the user.
 *
 * @param confidenceScore 0.0 (low confidence, review carefully) .. 1.0 (near-certain, e.g. byte-identical duplicate).
 * @param explanation Human-readable "Why am I seeing this?" string — required for user trust in an
 *   automated deletion recommendation.
 */
data class SmartInsight(
    val id: String,
    val title: String,
    val description: String,
    val type: InsightType,
    val reclaimableBytes: Long,
    val affectedItemsCount: Int,
    val items: List<MediaItem>,
    val confidenceScore: Float,
    val explanation: String
) {
    init {
        require(confidenceScore in 0f..1f) {
            "confidenceScore must be within 0.0..1.0, was $confidenceScore"
        }
        require(affectedItemsCount >= 0) { "affectedItemsCount cannot be negative" }
    }
}