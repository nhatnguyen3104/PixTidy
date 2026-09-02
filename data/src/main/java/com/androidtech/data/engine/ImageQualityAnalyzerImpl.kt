package com.androidtech.data.engine

import android.graphics.Bitmap
import com.androidtech.data.source.MediaStoreDataSource
import com.androidtech.domain.model.media.ImageQualityScore
import com.androidtech.domain.model.media.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Estimates image quality using classic, model-free signal-processing heuristics:
 * a Laplacian-variance sharpness estimate (blur detection) and mean-luminance darkness
 * detection. Deliberately avoids any ML/TFLite dependency to keep the MVP lightweight
 * and fully deterministic.
 */
class ImageQualityAnalyzerImpl @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource
) {

    suspend fun analyze(item: MediaItem): ImageQualityScore = withContext(Dispatchers.Default) {
        val bitmap = mediaStoreDataSource.loadThumbnail(item, ANALYSIS_SIZE_PX)
            ?: return@withContext ImageQualityScore(
                overallScore = 0,
                blurrinessScore = 0f,
                isBlurry = true,
                isDark = false
            )

        try {
            val gray = toGrayscale(bitmap)
            val variance = laplacianVariance(gray, bitmap.width, bitmap.height)
            val meanLuminance = gray.average()

            val isBlurry = variance < BLUR_VARIANCE_THRESHOLD
            val isDark = meanLuminance < DARK_LUMINANCE_THRESHOLD

            val sharpnessComponent = (variance / BLUR_VARIANCE_NORMALIZER).coerceIn(0.0, 1.0)
            val darkPenalty = if (isDark) DARK_PENALTY_FRACTION else 0.0
            val overallScore = ((sharpnessComponent - darkPenalty) * 100)
                .coerceIn(0.0, 100.0)
                .toInt()

            ImageQualityScore(
                overallScore = overallScore,
                blurrinessScore = variance.toFloat(),
                isBlurry = isBlurry,
                isDark = isDark
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun toGrayscale(bitmap: Bitmap): DoubleArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return DoubleArray(pixels.size) { i ->
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            r * 0.299 + g * 0.587 + b * 0.114
        }
    }

    /**
     * Approximates a discrete Laplacian (4-neighbor second-derivative edge operator) over the
     * grayscale image and returns the variance of its response across all interior pixels.
     * Sharp, in-focus photos have strong high-frequency edges and thus high variance; blurry
     * photos have smoothed-out edges and low variance. This is the standard "variance of
     * Laplacian" blur metric, computed here without a full convolution framework.
     */
    private fun laplacianVariance(gray: DoubleArray, width: Int, height: Int): Double {
        if (width < 3 || height < 3) return 0.0

        val responses = DoubleArray((width - 2) * (height - 2))
        var idx = 0
        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val center = gray[rowOffset + x]
                val up = gray[rowOffset - width + x]
                val down = gray[rowOffset + width + x]
                val left = gray[rowOffset + x - 1]
                val right = gray[rowOffset + x + 1]
                responses[idx++] = (up + down + left + right) - (4 * center)
            }
        }

        val mean = responses.average()
        var sumSquaredDiff = 0.0
        for (v in responses) {
            val diff = v - mean
            sumSquaredDiff += diff * diff
        }
        return sumSquaredDiff / responses.size
    }

    companion object {
        private const val ANALYSIS_SIZE_PX = 256
        private const val BLUR_VARIANCE_THRESHOLD = 100.0
        private const val BLUR_VARIANCE_NORMALIZER = 500.0
        private const val DARK_LUMINANCE_THRESHOLD = 50.0
        private const val DARK_PENALTY_FRACTION = 0.25
    }
}
