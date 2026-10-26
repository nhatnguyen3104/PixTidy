package com.androidtech.domain.utils

/**
 * Pure-Kotlin operations on hex-encoded perceptual hashes (dHash/pHash strings produced by the
 * data layer). Lives in domain because it is plain bit arithmetic with zero Android dependency,
 * and both [com.androidtech.domain.usecase.RunSmartScanUseCase] and
 * [com.androidtech.domain.usecase.RunDeepSmartScanUseCase] need it for clustering.
 */
object PerceptualHashUtils {

    /**
     * Hamming distance between two equal-length hex hash strings, i.e. the number of differing
     * bits. Returns [Int.MAX_VALUE] if the hashes are null, blank, or of mismatched length
     * (treated as "cannot be compared" rather than throwing, so callers can filter safely).
     */
    fun hammingDistance(a: String?, b: String?): Int {
        if (a.isNullOrBlank() || b.isNullOrBlank() || a.length != b.length) return Int.MAX_VALUE
        var distance = 0
        for (i in a.indices) {
            val xor = Character.digit(a[i], 16) xor Character.digit(b[i], 16)
            distance += Integer.bitCount(xor)
        }
        return distance
    }

    /**
     * Converts a raw Hamming distance over a hash of [hashBits] bits into a 0..100 similarity
     * percentage, e.g. distance 0 -> 100%, distance == hashBits -> 0%.
     */
    fun similarityPercentage(distance: Int, hashBits: Int = 64): Int {
        if (distance == Int.MAX_VALUE || hashBits <= 0) return 0
        val clamped = distance.coerceIn(0, hashBits)
        return (100 - (clamped * 100 / hashBits)).coerceIn(0, 100)
    }
}