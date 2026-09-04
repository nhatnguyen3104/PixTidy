package com.androidtech.domain.engine

import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.SimilarGroup
import com.androidtech.domain.model.media.SmartInsight

/** Turns raw pipeline findings into ranked, explainable, user-facing [SmartInsight]s. */
interface RecommendationEngine {

    /**
     * @param duplicates groups of byte-identical items (each inner list has size >= 2)
     * @param similarGroups clusters of perceptually-similar-but-not-identical items
     * @param blurry items whose [com.androidtech.domain.model.media.QualityScore.isBlurry] is true and
     *   which are not already covered by [duplicates] or [similarGroups]
     * @param screenshots items classified as screenshots
     * @param largeVideos videos above the large-video size threshold
     */
    fun generateInsights(
        duplicates: List<List<MediaItem>>,
        similarGroups: List<SimilarGroup>,
        blurry: List<MediaItem>,
        screenshots: List<MediaItem>,
        largeVideos: List<MediaItem>
    ): List<SmartInsight>
}