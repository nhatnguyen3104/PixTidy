package com.androidtech.data.engine

import android.graphics.Bitmap
import com.androidtech.data.source.MediaStoreDataSource
import com.androidtech.domain.model.media.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.core.graphics.scale
import androidx.core.graphics.get
import javax.inject.Inject

/**
 * Computes lightweight 64-bit perceptual fingerprints ("difference hash" / dHash) for
 * images, used to cluster visually similar photos (e.g. burst shots, near-duplicate
 * screenshots) without any ML model — keeping the whole pipeline on-device and cheap.
 */
class PerceptualHashEngineImpl @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource
) {

    /**
     * Computes a 64-bit dHash for [item].
     *
     * Algorithm:
     * 1. Load a downscaled thumbnail (already memory-safe via [MediaStoreDataSource.loadThumbnail]).
     * 2. Resize it to a tiny 9x8 grid — one pixel wider than the target hash so we can compare
     *    each pixel to its immediate right-hand neighbor.
     * 3. Convert to grayscale luminance and, for each of the 64 (8x8) adjacent pixel pairs,
     *    emit a `1` bit if the left pixel is brighter than the right, else `0`.
     *
     * This makes the hash invariant to overall exposure shifts, small recompression artifacts,
     * and minor resizing — exactly the kind of near-duplicate variance found in camera roll
     * burst shots — while remaining sensitive to actual structural/content differences.
     *
     * Returns null if the thumbnail could not be decoded (e.g. missing/corrupt file).
     */
    suspend fun computeDHash(item: MediaItem): String? = withContext(Dispatchers.Default) {
        val source = mediaStoreDataSource.loadThumbnail(item, HASH_SOURCE_SIZE_PX) ?: return@withContext null
        try {
            val scaled = source.scale(HASH_WIDTH + 1, HASH_HEIGHT)
            try {
                val bits = StringBuilder(HASH_WIDTH * HASH_HEIGHT)
                for (y in 0 until HASH_HEIGHT) {
                    for (x in 0 until HASH_WIDTH) {
                        val leftLuma = luminance(scaled[x, y])
                        val rightLuma = luminance(scaled[x + 1, y])
                        bits.append(if (leftLuma > rightLuma) '1' else '0')
                    }
                }
                bitsToHex(bits.toString())
            } finally {
                if (scaled !== source) scaled.recycle()
            }
        } finally {
            source.recycle()
        }
    }

    private fun luminance(pixel: Int): Int {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        // Rec. 601 luma weights, integer arithmetic to avoid float overhead per-pixel.
        return (r * 299 + g * 587 + b * 114) / 1000
    }

    private fun bitsToHex(bits: String): String {
        val hex = StringBuilder(bits.length / 4)
        for (chunkStart in bits.indices step 4) {
            val nibble = bits.substring(chunkStart, minOf(chunkStart + 4, bits.length)).padEnd(4, '0')
            hex.append(Integer.toHexString(nibble.toInt(2)))
        }
        return hex.toString()
    }

    companion object {
        private const val HASH_WIDTH = 8
        private const val HASH_HEIGHT = 8
        private const val HASH_SOURCE_SIZE_PX = 256

        /** Total bits in a fingerprint; used to normalize Hamming distance into a similarity percentage. */
        const val HASH_BIT_LENGTH = HASH_WIDTH * HASH_HEIGHT

        /**
         * Number of differing bits between two equal-length hex-encoded hashes.
         * Returns [Int.MAX_VALUE] for mismatched lengths, which downstream clustering
         * logic treats as "never similar".
         */
        fun hammingDistance(hashA: String, hashB: String): Int {
            if (hashA.length != hashB.length) return Int.MAX_VALUE
            var distance = 0
            for (i in hashA.indices) {
                val nibbleA = Character.digit(hashA[i], 16)
                val nibbleB = Character.digit(hashB[i], 16)
                distance += Integer.bitCount(nibbleA xor nibbleB)
            }
            return distance
        }
    }
}
