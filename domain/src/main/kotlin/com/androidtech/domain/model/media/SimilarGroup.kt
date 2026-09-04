package com.androidtech.domain.model.media

/**
 * A cluster of near-duplicate (not byte-identical) images, e.g. burst shots or re-saves.
 *
 * @param bestCandidateId id of the [MediaItem] within [items] recommended to keep.
 * @param similarityPercentage 0..100, the cluster's minimum pairwise similarity.
 * @param potentialSavingsBytes bytes reclaimable if every item except [bestCandidateId] is deleted.
 */
data class SimilarGroup(
    val groupId: String,
    val items: List<MediaItem>,
    val bestCandidateId: Long,
    val similarityPercentage: Int,
    val potentialSavingsBytes: Long
) {
    init {
        require(items.isNotEmpty()) { "SimilarGroup '$groupId' must contain at least one item" }
        require(items.any { it.id == bestCandidateId }) {
            "bestCandidateId $bestCandidateId must reference an item within the group"
        }
    }
}