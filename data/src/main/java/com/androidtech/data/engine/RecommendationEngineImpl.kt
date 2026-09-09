package com.androidtech.data.engine

import com.androidtech.data.logger.DataLogger
import com.androidtech.domain.engine.RecommendationEngine
import com.androidtech.domain.model.media.DuplicateGroup
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.SimilarGroup
import com.androidtech.domain.model.media.SmartInsight
import com.androidtech.domain.model.media.SmartInsightType
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Deterministic, rules-based recommendation engine — intentionally not ML-based, so every
 * recommendation is explainable and reproducible (auditable "Why?" strings).
 *
 * Confidence bands are a hard product rule, not a tuning knob:
 *  - [SmartInsightType.EXACT_DUPLICATES]: always >= [EXACT_DUPLICATE_CONFIDENCE] (0.98) — a content
 *    hash match is byte-identical, so this is presented as safe-to-clean.
 *  - [SmartInsightType.SIMILAR_PHOTOS]: always within [SIMILAR_CONFIDENCE_MIN]..[SIMILAR_CONFIDENCE_MAX]
 *    (0.80..0.88) and always surfaced as "Needs Review" in its description/explanation — visual
 *    similarity is probabilistic, so this insight type is never presented as safe-to-clean.
 */
class RecommendationEngineImpl @Inject constructor() : RecommendationEngine {

    override fun generateInsights(
        duplicates: List<DuplicateGroup>,
        similarGroups: List<SimilarGroup>,
        blurry: List<MediaItem>,
        screenshots: List<MediaItem>,
        largeVideos: List<MediaItem>
    ): List<SmartInsight> = buildList {
        buildExactDuplicateInsight(duplicates)?.let(::add)
        buildSimilarPhotosInsight(similarGroups)?.let(::add)
        buildBlurryInsight(blurry)?.let(::add)
        buildScreenshotInsight(screenshots)?.let(::add)
        buildLargeVideoInsight(largeVideos)?.let(::add)
    }

    // ---- Exact duplicates: confidence always >= 0.98, safe-to-clean --------------------------

    private fun buildExactDuplicateInsight(duplicateGroups: List<DuplicateGroup>): SmartInsight? {
        if (duplicateGroups.isEmpty()) return null

        val allItems = duplicateGroups.flatMap { group ->
            group.allItems.map { item ->
                item.copy(
                    groupId = group.groupId,
                    isBestKeep = item.id == group.originalItem.id
                )
            }
        }
        val toDeleteCount = duplicateGroups.sumOf { it.duplicateItems.size }
        val reclaimableBytes = duplicateGroups.sumOf { it.totalSizeDifferenceBytes }

        return SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Exact duplicates",
            description = "$toDeleteCount file(s) across ${duplicateGroups.size} group(s) are byte-for-byte " +
                    "identical copies of another file you already have.",
            type = SmartInsightType.EXACT_DUPLICATES,
            reclaimableBytes = reclaimableBytes,
            affectedItemsCount = toDeleteCount,
            items = allItems,
            confidenceScore = EXACT_DUPLICATE_CONFIDENCE,
            explanation = "These files share an identical content hash (SHA-256), meaning they are " +
                    "pixel-for-pixel, byte-for-byte the same file — there is no meaningful difference to " +
                    "review. Safe to clean. The oldest (or, if tied, highest-resolution) copy of each is kept."
        )
    }

    // ---- Similar (not identical) photos: confidence 0.80-0.88, always "Needs Review" ---------

    private fun buildSimilarPhotosInsight(groups: List<SimilarGroup>): SmartInsight? {
        if (groups.isEmpty()) return null

        val allItems = groups.flatMap { group ->
            group.items.map { item ->
                item.copy(
                    groupId = group.groupId,
                    isBestKeep = item.id == group.bestCandidateId
                )
            }
        }
        val toDelete = groups.flatMap { group -> group.items.filterNot { it.id == group.bestCandidateId } }
        val avgConfidence = groups.map(::confidenceFor).average().toFloat()

        return SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Similar photos — Needs Review",
            description = "${toDelete.size} photo(s) across ${groups.size} group(s) look very similar to " +
                    "another photo you kept. Needs review before deleting — these are visually similar, not " +
                    "guaranteed identical.",
            type = SmartInsightType.SIMILAR_PHOTOS,
            reclaimableBytes = toDelete.sumOf { it.size },
            affectedItemsCount = toDelete.size,
            items = allItems,
            confidenceScore = avgConfidence,
            explanation = "Compared visual fingerprints (perceptual hashing) across your library. Each " +
                    "group shares a strong visual match — likely burst shots or re-saves — and the sharpest, " +
                    "highest-resolution copy in each group is recommended to keep. Unlike an exact duplicate, " +
                    "this is a probabilistic match, not a byte-for-byte one — please review before deleting."
        )
    }

    /** Maps 90-99% similarity linearly onto the 0.80-0.88 confidence band; clamped either end. */
    private fun confidenceFor(group: SimilarGroup): Float {
        val clamped = group.similarityPercentage.coerceIn(SIMILARITY_BAND_MIN, SIMILARITY_BAND_MAX)
        val t = (clamped - SIMILARITY_BAND_MIN).toFloat() / (SIMILARITY_BAND_MAX - SIMILARITY_BAND_MIN)
        return SIMILAR_CONFIDENCE_MIN + t * (SIMILAR_CONFIDENCE_MAX - SIMILAR_CONFIDENCE_MIN)
    }

    // ---- Blurry / low-quality standalone photos --------------------------------------------------

    private fun buildBlurryInsight(blurry: List<MediaItem>): SmartInsight? {
        if (blurry.isEmpty()) return null

        return SmartInsight(
            id = UUID.randomUUID().toString(),
            title = "Blurry photos",
            description = "${blurry.size} photo(s) appear out of focus or otherwise low quality.",
            type = SmartInsightType.BLURRY_PHOTOS,
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
            type = SmartInsightType.SCREENSHOTS,
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
            type = SmartInsightType.LARGE_VIDEOS,
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
        const val SIMILAR_CONFIDENCE_MIN = 0.80f
        const val SIMILAR_CONFIDENCE_MAX = 0.88f
        const val SIMILARITY_BAND_MIN = 90
        const val SIMILARITY_BAND_MAX = 99
        const val BLURRY_STANDALONE_CONFIDENCE = 0.55f
        const val OLD_SCREENSHOT_CONFIDENCE = 0.75f
        const val LARGE_VIDEO_CONFIDENCE = 0.30f
        const val SCREENSHOT_AGE_THRESHOLD_DAYS = 30L
    }
}
