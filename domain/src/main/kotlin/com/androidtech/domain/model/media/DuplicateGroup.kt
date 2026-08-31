package com.androidtech.domain.model.media

/**
 * A set of [MediaItem]s judged to be duplicates (or near-duplicates,
 * depending on the detector's strategy — see [DuplicateDetectorRepository]).
 *
 * A group must contain at least two items to be meaningful; constructing a
 * group with fewer than two is a caller error and detectors should never
 * emit one.
 *
 * @property groupHash Stable identifier for this group, derived by the
 *   detector (e.g. a perceptual hash or content hash). Two scans that find
 *   the same duplicate set should produce the same [groupHash], which lets
 *   the UI layer diff scan results across runs.
 * @property totalSizeBytes Combined size, in bytes, of every item in [items].
 * @property items The duplicate items themselves. Order is not significant;
 *   use [recommendedToKeepId] to find the suggested "original".
 * @property recommendedToKeepId The [MediaItem.id] the detector suggests
 *   keeping (e.g. highest resolution, earliest date added). Must correspond
 *   to one of the ids present in [items]. All other items in the group are
 *   the ones a "clean up duplicates" action would delete.
 */
data class DuplicateGroup(
    val groupHash: String,
    val totalSizeBytes: Long,
    val items: List<MediaItem>,
    val recommendedToKeepId: Long
) {
    init {
        require(items.size >= 2) { "A DuplicateGroup must contain at least two items." }
        require(items.any { it.id == recommendedToKeepId }) {
            "recommendedToKeepId ($recommendedToKeepId) must reference an item in items."
        }
    }

    /** Items the group recommends removing, i.e. everything but [recommendedToKeepId]. */
    val removableItems: List<MediaItem>
        get() = items.filterNot { it.id == recommendedToKeepId }

    /** Bytes reclaimable if every [removableItems] entry were deleted. */
    val reclaimableSizeBytes: Long
        get() = removableItems.sumOf { it.sizeBytes }
}