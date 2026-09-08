package com.androidtech.domain.engine

import com.androidtech.domain.model.media.DuplicateGroup
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.SimilarGroup
import com.androidtech.domain.model.media.SmartInsight

/** Turns raw pipeline findings into ranked, explainable, user-facing [SmartInsight]s. */
interface RecommendationEngine {

    /**
     * @param duplicates byte-identical clusters — always scored 0.98+ confidence, safe-to-clean
     * @param similarGroups visually-similar-but-not-identical clusters — always scored ~0.80-0.88
     *   confidence and surfaced as "Needs Review", never as safe-to-clean
     * @param blurry items whose [com.androidtech.domain.model.media.QualityScore.isBlurry] is true and
     *   which are not already covered by [duplicates] or [similarGroups]
     * @param screenshots items classified as screenshots
     * @param largeVideos videos above the large-video size threshold
     */
    fun generateInsights(
        duplicates: List<DuplicateGroup>,
        similarGroups: List<SimilarGroup>,
        blurry: List<MediaItem>,
        screenshots: List<MediaItem>,
        largeVideos: List<MediaItem>
    ): List<SmartInsight>
}