package com.androidtech.data.utils

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale

/**
 * Shared bitmap decoding + dHash computation, used by both
 * [com.androidtech.data.analyzer.FastPerceptualHashAnalyzer] (still images) and
 * [com.androidtech.data.engine.DeepAnalysisEngineImpl] (video frames) so the hashing logic — and
 * its memory-safety characteristics — has exactly one implementation.
 */
internal object BitmapHashUtils {

    /** 8x8 dHash: 9x8 grayscale samples compared column-adjacently -> 64 bits. */
    private const val HASH_WIDTH = 9
    private const val HASH_HEIGHT = 8

    /**
     * Decodes [uri] at a downsampled resolution (never full-size — this is the primary OOM guard
     * for large photos) and returns its dHash as a 16-char hex string, or null on any failure.
     */
    fun computeDHashFromUri(resolver: ContentResolver, uri: Uri): String? {
        val downsampled = decodeDownsampled(resolver, uri, targetMaxDimension = DECODE_TARGET_MAX_DIMENSION)
            ?: return null
        return try {
            computeDHash(downsampled)
        } finally {
            downsampled.recycle()
        }
    }

    /** Computes a dHash directly from an already-decoded [Bitmap] (e.g. an extracted video frame). */
    fun computeDHash(bitmap: Bitmap): String {
        val small = bitmap.scale(HASH_WIDTH, HASH_HEIGHT)
        try {
            val gray = IntArray(HASH_WIDTH * HASH_HEIGHT)
            small.getPixels(gray, 0, HASH_WIDTH, 0, 0, HASH_WIDTH, HASH_HEIGHT)

            val luminance = IntArray(gray.size) { i -> luminanceOf(gray[i]) }

            var bits = 0L
            var bitIndex = 0
            for (row in 0 until HASH_HEIGHT) {
                for (col in 0 until HASH_WIDTH - 1) {
                    val left = luminance[row * HASH_WIDTH + col]
                    val right = luminance[row * HASH_WIDTH + col + 1]
                    if (left > right) bits = bits or (1L shl bitIndex)
                    bitIndex++
                }
            }
            return "%016x".format(bits)
        } finally {
            if (small !== bitmap) small.recycle()
        }
    }

    /**
     * Two-pass decode: first read bounds only (`inJustDecodeBounds`), compute the smallest
     * power-of-two `inSampleSize` that gets us at or below [targetMaxDimension], then decode for
     * real. This keeps peak native memory bounded regardless of the source image's resolution.
     */
    fun decodeDownsampled(resolver: ContentResolver, uri: Uri, targetMaxDimension: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, bounds)
            } ?: return null

            val sampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, targetMaxDimension)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // halves memory vs ARGB_8888; fine for hashing/quality
            }
            resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            }
        } catch (e: OutOfMemoryError) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(sourceWidth: Int, sourceHeight: Int, targetMaxDimension: Int): Int {
        var sampleSize = 1
        var width = sourceWidth
        var height = sourceHeight
        while (width / 2 >= targetMaxDimension || height / 2 >= targetMaxDimension) {
            width /= 2
            height /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun luminanceOf(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }

    private const val DECODE_TARGET_MAX_DIMENSION = 64 // comfortably above HASH_WIDTH/HASH_HEIGHT for quality downscale
}
