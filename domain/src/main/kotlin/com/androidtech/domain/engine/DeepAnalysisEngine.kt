package com.androidtech.domain.engine

import com.androidtech.domain.model.media.DocumentAnalysisResult
import com.androidtech.domain.model.media.FrameSample
import com.androidtech.domain.model.media.MediaItem

/**
 * ADDITION beyond the original interface spec: Deep Smart Scan's "Deep Frame Extraction for
 * screen recordings" and "OCR/Document detection" have no home in [ImageAnalyzerEngine] (which is
 * still-image only) or [RecommendationEngine] (which is stateless scoring). This interface is the
 * natural seam for both, kept separate so a lighter Smart Scan build can omit the heavier
 * MediaMetadataRetriever / on-device OCR dependencies this implies.
 */
interface DeepAnalysisEngine {

    /**
     * Samples up to [maxFrames] evenly-spaced frames from a video and returns their perceptual
     * hashes only (never raw Bitmaps — keeps the domain module pure). Used for deep duplicate
     * video detection and screen-recording comparison. Returns an empty list for non-video items
     * or on extraction failure.
     */
    suspend fun sampleKeyFrames(item: MediaItem, maxFrames: Int = DEFAULT_MAX_FRAMES): List<FrameSample>

    /**
     * Runs on-device OCR/document detection on an image to identify receipts, whiteboards,
     * screenshots of text, etc.
     */
    suspend fun analyzeDocument(item: MediaItem): DocumentAnalysisResult

    companion object {
        const val DEFAULT_MAX_FRAMES = 5
    }
}