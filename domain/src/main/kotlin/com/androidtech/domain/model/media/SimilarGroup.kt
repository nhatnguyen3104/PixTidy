package com.androidtech.domain.model.media

data class SimilarGroup(
    val groupId: String,
    val visualHash: String?,
    val similarityPercentage: Int,
    val totalSizeBytes: Long,
    val items: List<MediaItem>,
    val recommendedKeepItemId: Long?,
    val reasonForRecommendation: String
)