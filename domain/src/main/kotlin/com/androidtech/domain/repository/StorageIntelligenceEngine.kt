package com.androidtech.domain.repository

import com.androidtech.domain.model.media.ImageQualityScore
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.SimilarGroup

/**
 * On-device intelligence for finding duplicate/similar media and assessing image quality.
 * All analysis is local — no media data ever leaves the device.
 */
interface StorageIntelligenceEngine {

    /** Groups items that are byte-for-byte identical (same content hash). */
    suspend fun detectExactDuplicates(items: List<MediaItem>): List<SimilarGroup>

    /**
     * Groups items that are visually similar (e.g. burst-shot photos, screenshots of the
     * same content) using perceptual hashing.
     *
     * @param threshold Minimum similarity, in the range [0f, 1f], required to cluster two
     *   items together. Higher values require closer visual matches.
     */
    suspend fun detectSimilarPhotos(items: List<MediaItem>, threshold: Float = 0.85f): List<SimilarGroup>

    /** Produces a heuristic sharpness/exposure quality assessment for a single image. */
    suspend fun analyzeImageQuality(item: MediaItem): ImageQualityScore
}