package com.androidtech.data.engine

import com.androidtech.data.logger.DataLogger
import com.androidtech.domain.engine.RecommendationEngine
import com.androidtech.domain.model.media.InsightType
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.SimilarGroup
import com.androidtech.domain.model.media.SmartInsight
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Deterministic, rules-based recommendation engine — intentionally not ML-based, so every
 * recommendation is explainable and reproducible (auditable "Why?" strings, per the spec).
 *
 * Convention used throughout: [SmartInsight.items] holds every item relevant for user review
 * (including the recommended "keeper" in a duplicate/similar group), while
 * [SmartInsight.affectedItemsCount] / [SmartInsight.reclaimableBytes] describe only the items
 * actually recommended *for deletion* within that group.
 */
class RecommendationEngineImpl @Inject constructor() : RecommendationEngine {

    override fun generateInsights(
        duplicates: List<List<MediaItem>>,
        similarGroups: List<SimilarGroup>,
        blurry: List<MediaItem>,
        screenshots: List<MediaItem>,
        largeVideos: List<MediaItem>
    ): List<SmartInsight> {
        DataLogger.d(
            "generateInsights: duplicates=${duplicates.size}, similar=${similarGroups.size}, " +
                    "blurry=${blurry.size}, screenshots=${screenshots.size}, largeVideos=${largeVideos.size}"
        )
        val insights = buildList {
            buildExactDuplicateInsight(duplicates)?.let(::add)
            buildSimilarPhotosInsight(similarGroups, blurryIds = blurry.mapTo(HashSet()) { it.id })?.let(::add)
            buildBlurryInsight(blurry)?.let(::add)
            buildScreenshotInsight(screenshots)?.let(::add)
            buildLargeVideoInsight(largeVideos)?.let(::add)
        }
        DataLogger.d("generateInsights: produced ${insights.size} insights")
        return insights
    }

    // ---- Exact duplicates: confidence 0.98 -----------------------------------------------------

    private fun buildExactDuplicateInsight(duplicateGroups: List<List<MediaItem>>): SmartInsight? {
        if (duplicateGroups.isEmpty()) return null

        val allItems = mutableListOf<MediaItem>()
        val toDelete = mutableListOf<MediaItem>()
        for (group in duplicateGroups) {
            val keeper = group.minBy { it.dateAdded } // keep the earliest-added copy as "the original"
            allItems += group
            toDelete += group.filterNot { it.id == keeper.id }
        }

        return SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Exact duplicates",
            description = "${toDelete.size} file(s) are byte-for-byte identical copies of another file you already have.",
            type = InsightType.EXACT_DUPLICATE,
            reclaimableBytes = toDelete.sumOf { it.size },
            affectedItemsCount = toDelete.size,
            items = allItems,
            confidenceScore = EXACT_DUPLICATE_CONFIDENCE,
            explanation = "These files produced an identical content hash (SHA-256), meaning they are " +
                    "pixel-for-pixel, byte-for-byte the same file. The oldest copy of each is kept."
        )
    }

    // ---- Similar (not identical) photos: >90% match + low quality -> 0.85 ----------------------

    private fun buildSimilarPhotosInsight(groups: List<SimilarGroup>, blurryIds: Set<Long>): SmartInsight? {
        if (groups.isEmpty()) return null

        val allItems = mutableListOf<MediaItem>()
        val toDelete = mutableListOf<MediaItem>()
        var confidenceSum = 0f

        for (group in groups) {
            allItems += group.items
            val deletable = group.items.filterNot { it.id == group.bestCandidateId }
            toDelete += deletable
            confidenceSum += confidenceFor(group, blurryIds)
        }

        val avgConfidence = if (groups.isNotEmpty()) confidenceSum / groups.size else 0f

        return SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Similar photos",
            description = "${toDelete.size} photo(s) across ${groups.size} group(s) look very similar to another photo you kept.",
            type = InsightType.SIMILAR_PHOTOS,
            reclaimableBytes = toDelete.sumOf { it.size },
            affectedItemsCount = toDelete.size,
            items = allItems,
            confidenceScore = avgConfidence,
            explanation = "Compared visual fingerprints (perceptual hashing) across your library. Each " +
                    "group shares a strong visual match — likely burst shots or re-saves — and the sharpest, " +
                    "highest-resolution copy in each group is kept."
        )
    }

    private fun confidenceFor(group: SimilarGroup, blurryIds: Set<Long>): Float {
        val hasLowQualityMember = group.items.any { it.id in blurryIds }
        return when {
            group.similarityPercentage > SIMILARITY_HIGH_CONFIDENCE_THRESHOLD && hasLowQualityMember ->
                SIMILAR_PLUS_LOW_QUALITY_CONFIDENCE
            group.similarityPercentage > SIMILARITY_HIGH_CONFIDENCE_THRESHOLD ->
                SIMILARITY_ONLY_CONFIDENCE
            else ->
                (group.similarityPercentage / 100f) * SIMILARITY_SCALED_CONFIDENCE_CAP
        }
    }

    // ---- Blurry / low-quality standalone photos --------------------------------------------------

    private fun buildBlurryInsight(blurry: List<MediaItem>): SmartInsight? {
        if (blurry.isEmpty()) return null

        return SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Blurry photos",
            description = "${blurry.size} photo(s) appear out of focus or otherwise low quality.",
            type = InsightType.BLURRY_PHOTOS,
            reclaimableBytes = blurry.sumOf { it.size },
            affectedItemsCount = blurry.size,
            items = blurry,
            confidenceScore = BLURRY_STANDALONE_CONFIDENCE,
            explanation = "A sharpness analysis (Laplacian variance) flagged these as noticeably soft or " +
                    "out of focus. Not a duplicate of anything else you have — review before deleting."
        )
    }

    // ---- Screenshots older than 30 days -> 0.75 --------------------------------------------------

    private fun buildScreenshotInsight(screenshots: List<MediaItem>): SmartInsight? {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(SCREENSHOT_AGE_THRESHOLD_DAYS)
        val oldScreenshots = screenshots.filter { it.dateAdded < cutoff }
        if (oldScreenshots.isEmpty()) return null

        return SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Old screenshots",
            description = "${oldScreenshots.size} screenshot(s) are more than $SCREENSHOT_AGE_THRESHOLD_DAYS days old.",
            type = InsightType.SCREENSHOTS,
            reclaimableBytes = oldScreenshots.sumOf { it.size },
            affectedItemsCount = oldScreenshots.size,
            items = oldScreenshots,
            confidenceScore = OLD_SCREENSHOT_CONFIDENCE,
            explanation = "Screenshots tend to lose relevance quickly. These have been sitting in your " +
                    "library for over $SCREENSHOT_AGE_THRESHOLD_DAYS days without being revisited."
        )
    }

    // ---- Large videos (informational, not "junk") -------------------------------------------------

    private fun buildLargeVideoInsight(largeVideos: List<MediaItem>): SmartInsight? {
        if (largeVideos.isEmpty()) return null

        return SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Large videos",
            description = "${largeVideos.size} video(s) are taking up a large share of your storage.",
            type = InsightType.LARGE_VIDEOS,
            reclaimableBytes = largeVideos.sumOf { it.size },
            affectedItemsCount = largeVideos.size,
            items = largeVideos,
            confidenceScore = LARGE_VIDEO_CONFIDENCE,
            explanation = "These are simply your biggest video files by size — nothing here is flagged " +
                    "as unwanted. Consider backing up before deleting anything you'd want to keep."
        )
    }

    private companion object {
        const val EXACT_DUPLICATE_CONFIDENCE = 0.98f
        const val SIMILAR_PLUS_LOW_QUALITY_CONFIDENCE = 0.85f
        const val SIMILARITY_ONLY_CONFIDENCE = 0.70f
        const val SIMILARITY_SCALED_CONFIDENCE_CAP = 0.60f
        const val SIMILARITY_HIGH_CONFIDENCE_THRESHOLD = 90
        const val BLURRY_STANDALONE_CONFIDENCE = 0.55f
        const val OLD_SCREENSHOT_CONFIDENCE = 0.75f
        const val LARGE_VIDEO_CONFIDENCE = 0.30f
        const val SCREENSHOT_AGE_THRESHOLD_DAYS = 30L
    }
}
