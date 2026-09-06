package com.androidtech.domain.repository

import com.androidtech.domain.model.media.CachedMediaAnalysis

/**
 * Abstraction over the Smart Scan analysis cache. A cache entry is only ever considered valid if
 * both the media id AND the file's `dateModified` match the stored record — any edit, re-save, or
 * replace of the underlying file invalidates its cache automatically, since [getValidCache] takes
 * the caller's current [dateModified] and returns null on any mismatch rather than a stale hit.
 */
interface MediaAnalysisCacheRepository {

    /** Returns the cached analysis only if it exists AND its stored dateModified still matches. */
    suspend fun getValidCache(mediaId: Long, dateModified: Long): CachedMediaAnalysis?

    /** Upserts the analysis for [mediaId] as of [dateModified]. */
    suspend fun saveAnalysis(
        mediaId: Long,
        dateModified: Long,
        phash: String?,
        blurScore: Int,
        isScreenshot: Boolean
    )

    /** Removes cache rows for media no longer present on device (deleted/moved since last scan). */
    suspend fun pruneOrphaned(validMediaIds: List<Long>)
}