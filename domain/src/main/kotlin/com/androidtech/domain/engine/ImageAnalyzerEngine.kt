package com.androidtech.domain.engine

import com.androidtech.domain.model.media.DuplicateGroup
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.QualityScore
import com.androidtech.domain.model.media.SimilarGroup

/**
 * On-device image analysis used by Smart Scan.
 *
 * NOTE: the original spec listed only [calculatePhash] and [evaluateQuality]. [calculateContentHash]
 * was added because exact-duplicate detection (pipeline step b) requires it, and content hashing
 * is still "analyzing an image" — adding a third, narrowly-scoped interface for one method would
 * be more indirection than value.
 */
interface ImageAnalyzerEngine {

    /** Perceptual hash (dHash) for similarity clustering. Null if the item could not be decoded. */
    suspend fun calculatePhash(item: MediaItem): String?

    /** Sharpness/exposure evaluation. Never throws for a decode failure — see [QualityScore.UNKNOWN]. */
    suspend fun evaluateQuality(item: MediaItem): QualityScore

    /** Exact-content hash (SHA-256) for byte-identical duplicate detection. Null if unreadable. */
    suspend fun calculateContentHash(item: MediaItem): String?

    /**
     * Groups [candidates] into 100%-byte-identical clusters. Each returned [DuplicateGroup] has at
     * least one duplicate besides its [DuplicateGroup.originalItem]; candidates with no match are
     * simply absent from the result, not returned as singleton groups.
     */
    suspend fun findExactDuplicates(
        candidates: List<MediaItem>,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null
    ): List<DuplicateGroup>

    /**
     * Groups [candidates] into visually-similar (not byte-identical) clusters via perceptual
     * hashing. Candidates with no visual match are absent from the result. Implementations are
     * expected to use an incremental cache (mediaId + dateModified) so re-scanning unchanged files
     * is cheap.
     */
    suspend fun findSimilarGroups(
        candidates: List<MediaItem>,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null
    ): List<SimilarGroup>

    /**
     * Returns the subset of [candidates] whose [QualityScore.isBlurry] is true. Intended to be
     * called with whatever remains after [findExactDuplicates] and [findSimilarGroups] have
     * removed their own matches, so quality doesn't get evaluated for images already headed for a
     * duplicate/similarity-based recommendation.
     */
    suspend fun findBlurryItems(
        candidates: List<MediaItem>,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null
    ): List<MediaItem>
}