package com.androidtech.domain.repository

import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.ScanProgress
import kotlinx.coroutines.flow.Flow

/**
 * Analyzes a set of [MediaItem]s and identifies duplicate/near-duplicate
 * groups.
 *
 * The domain layer deliberately does not prescribe the detection strategy
 * (byte-exact hash, perceptual/pHash similarity, EXIF + size heuristics,
 * etc) — that's a data-layer implementation detail. This contract only
 * fixes the shape of the request and the streamed result.
 */
interface DuplicateDetectorRepository {

    /**
     * Scans [items] for duplicates, emitting [ScanProgress] updates as work
     * proceeds.
     *
     * The returned [Flow] is expected to emit zero or more [ScanProgress.Scanning]
     * updates followed by exactly one terminal state — either
     * [ScanProgress.Completed] or [ScanProgress.Error] — after which the flow
     * completes. Collectors should stop waiting for further duplicate-related
     * updates once a terminal state is received.
     *
     * @param items Candidate items to analyze. Callers typically pass the
     *   result of [MediaRepository.getMedia] for the media types they care
     *   about.
     */
    fun scanForDuplicates(items: List<MediaItem>): Flow<ScanProgress>

    /**
     * Cancels an in-flight scan started by [scanForDuplicates], if any.
     *
     * Implementations should cause the corresponding [Flow] to complete
     * (typically after emitting an [ScanProgress.Error] or simply completing
     * without a terminal success state) rather than leaving collectors
     * hanging indefinitely.
     */
    suspend fun cancelScan()
}