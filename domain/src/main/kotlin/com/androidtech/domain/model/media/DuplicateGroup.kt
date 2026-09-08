package com.androidtech.domain.model.media

/**
 * A cluster of byte-for-byte identical files (100% content-hash match) — unlike [SimilarGroup],
 * every item here is cryptographically guaranteed to be the same file, which is what allows
 * [com.androidtech.domain.engine.RecommendationEngine] to score these at 0.98+ confidence and treat
 * them as safe-to-clean rather than "needs review".
 *
 * @param originalItem the file recommended to keep — earliest creation date by default, with
 *   highest resolution as a tie-breaker (see [com.androidtech.domain.engine.ImageAnalyzerEngine]
 *   implementation for the exact selection rule).
 * @param duplicateItems every other file in the group — safe to remove, since [originalItem]
 *   already preserves the content.
 * @param totalSizeDifferenceBytes bytes reclaimable by deleting every item in [duplicateItems]
 *   (i.e. the sum of their sizes) while keeping [originalItem].
 * @param hash the shared content hash (SHA-256) all items in this group have in common.
 */
data class DuplicateGroup(
    val groupId: String,
    val originalItem: MediaItem,
    val duplicateItems: List<MediaItem>,
    val totalSizeDifferenceBytes: Long,
    val hash: String
) {
    init {
        require(duplicateItems.isNotEmpty()) {
            "DuplicateGroup '$groupId' must have at least one duplicate besides originalItem"
        }
        require(originalItem.id !in duplicateItems.map { it.id }) {
            "originalItem must not also appear in duplicateItems"
        }
    }

    /** Convenience view for callers that want every item in the group, keeper included. */
    val allItems: List<MediaItem> get() = listOf(originalItem) + duplicateItems

    /** List item duplicate selected */
    val selectedDuplicateItems: List<MediaItem> get() = duplicateItems.filter { it.isSelected }

    /** Count of item duplicate selected */
    val selectedDuplicateItemsCount: Int get() = duplicateItems.count {it.isSelected}

    /** Size of item duplicate selected */
    val totalSelectedSize: Long get() = selectedDuplicateItems.sumOf { it.size }
}