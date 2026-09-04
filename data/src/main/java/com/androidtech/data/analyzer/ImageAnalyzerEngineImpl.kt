package com.androidtech.data.analyzer

import com.androidtech.domain.engine.ImageAnalyzerEngine
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.QualityScore
import javax.inject.Inject

/**
 * Composition root for the domain [ImageAnalyzerEngine] contract — each concern (perceptual hash,
 * quality, content hash) is its own small, independently testable class; this class only wires
 * them to the interface.
 */
class ImageAnalyzerEngineImpl @Inject constructor(
    private val perceptualHashAnalyzer: FastPerceptualHashAnalyzer,
    private val blurDetector: BlurDetectorImpl,
    private val contentHasher: ContentHasher
) : ImageAnalyzerEngine {

    override suspend fun calculatePhash(item: MediaItem): String? =
        perceptualHashAnalyzer.computePhash(item)

    override suspend fun evaluateQuality(item: MediaItem): QualityScore =
        blurDetector.evaluate(item)

    override suspend fun calculateContentHash(item: MediaItem): String? =
        contentHasher.sha256(item)
}