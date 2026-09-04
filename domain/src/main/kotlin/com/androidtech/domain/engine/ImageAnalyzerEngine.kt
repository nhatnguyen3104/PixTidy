package com.androidtech.domain.engine

import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.QualityScore

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
}