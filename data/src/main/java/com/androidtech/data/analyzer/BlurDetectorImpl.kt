package com.androidtech.data.analyzer

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.androidtech.data.utils.BitmapHashUtils
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.QualityScore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.sqrt
import androidx.core.net.toUri

/**
 * Sharpness via a discrete Laplacian-variance approximation (fast, no native/NDK dependency) and
 * exposure via mean grayscale luminance. Both operate on a downsampled decode — full-resolution
 * convolution would be far too slow and memory-hungry to run across a whole photo library.
 */
class BlurDetectorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun evaluate(item: MediaItem): QualityScore = withContext(Dispatchers.Default) {
        val bitmap = try {
            BitmapHashUtils.decodeDownsampled(
                context.contentResolver,
                item.uri.toUri(),
                targetMaxDimension = ANALYSIS_MAX_DIMENSION
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            null
        } ?: return@withContext QualityScore.UNKNOWN

        try {
            evaluateBitmap(bitmap)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            QualityScore.UNKNOWN
        } finally {
            bitmap.recycle()
        }
    }

    private fun evaluateBitmap(bitmap: Bitmap): QualityScore {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 3 || height < 3) return QualityScore.UNKNOWN

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val luminance = IntArray(pixels.size) { i -> luminanceOf(pixels[i]) }

        val laplacianVariance = laplacianVarianceOf(luminance, width, height)
        val meanLuminance = luminance.average()

        val sharpnessScore = mapVarianceToScore(laplacianVariance)
        val isBlurry = laplacianVariance < BLUR_VARIANCE_THRESHOLD
        val isOverexposed = meanLuminance > OVEREXPOSED_LUMINANCE_THRESHOLD
        val isUnderexposed = meanLuminance < UNDEREXPOSED_LUMINANCE_THRESHOLD

        // Exposure problems cap the score even when technically "sharp" — a blown-out sky is
        // still a photo worth flagging.
        val exposurePenalty = if (isOverexposed || isUnderexposed) EXPOSURE_PENALTY else 0
        val finalScore = (sharpnessScore - exposurePenalty).coerceIn(0, 100)

        return QualityScore(
            score = finalScore,
            isBlurry = isBlurry,
            isOverexposed = isOverexposed,
            isUnderexposed = isUnderexposed
        )
    }

    /** Sum of squared Laplacian responses over interior pixels, normalized by pixel count. */
    private fun laplacianVarianceOf(luminance: IntArray, width: Int, height: Int): Double {
        var sum = 0.0
        var sumSquares = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val center = luminance[rowOffset + x]
                val up = luminance[rowOffset - width + x]
                val down = luminance[rowOffset + width + x]
                val left = luminance[rowOffset + x - 1]
                val right = luminance[rowOffset + x + 1]

                val laplacian = (4 * center - up - down - left - right).toDouble()
                sum += laplacian
                sumSquares += laplacian * laplacian
                count++
            }
        }

        if (count == 0) return 0.0
        val mean = sum / count
        return (sumSquares / count) - (mean * mean)
    }

    private fun mapVarianceToScore(variance: Double): Int {
        // Empirical normalization: variance beyond SCORE_SATURATION_VARIANCE is treated as
        // "maximally sharp" for scoring purposes; sqrt compresses the long tail of very sharp images.
        val normalized = sqrt(variance.coerceAtLeast(0.0)) / sqrt(SCORE_SATURATION_VARIANCE)
        return (normalized * 100).toInt().coerceIn(0, 100)
    }

    private fun luminanceOf(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    private companion object {
        const val ANALYSIS_MAX_DIMENSION = 256
        const val BLUR_VARIANCE_THRESHOLD = 90.0
        const val SCORE_SATURATION_VARIANCE = 2500.0
        const val OVEREXPOSED_LUMINANCE_THRESHOLD = 235.0
        const val UNDEREXPOSED_LUMINANCE_THRESHOLD = 20.0
        const val EXPOSURE_PENALTY = 25
    }
}
