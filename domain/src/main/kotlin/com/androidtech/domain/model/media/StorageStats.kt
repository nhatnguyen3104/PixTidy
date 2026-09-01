package com.androidtech.domain.model.media

/**
 * A point-in-time snapshot of device storage usage.
 *
 * [totalBytes], [usedBytes], and [freeBytes] describe the whole volume (the
 * numbers a system "Storage" screen would show), while the `*SizeBytes`
 * fields below break the *used* portion down by SnapSweep's own categories.
 * The category fields are not guaranteed to sum to [usedBytes] — the OS,
 * other apps, and uncategorized files also consume space.
 *
 * @property totalBytes Total capacity of the volume, in bytes.
 * @property usedBytes Bytes currently occupied on the volume.
 * @property freeBytes Bytes currently available on the volume.
 * @property photosSizeBytes Total bytes attributed to [MediaType.IMAGE] items.
 * @property videosSizeBytes Total bytes attributed to [MediaType.VIDEO] items.
 * @property screenshotsSizeBytes Total bytes attributed to
 *   [MediaType.SCREENSHOT] items.
 * @property duplicatesSizeBytes Total bytes that could be reclaimed by
 *   resolving detected duplicate groups (i.e. the size of all
 *   non-recommended-to-keep items across all [DuplicateGroup]s).
 * @property otherSizeBytes Bytes used by files SnapSweep tracks but that
 *   don't fall into the categories above (e.g. documents, downloads, app
 *   caches), if the app chooses to surface them.
 */
data class StorageStats(
    /** Total commercial storage capacity (e.g., 64GB, 128GB, 256GB in bytes) */
    val totalBytes: Long,

    /** Total used space = System/OS + User files */
    val usedBytes: Long,

    /** Actual free space available for the user */
    val freeBytes: Long,

    // Categorized User Data
    val photosSizeBytes: Long = 0L,
    val videosSizeBytes: Long = 0L,
    val screenshotsSizeBytes: Long = 0L,
    val duplicatesSizeBytes: Long = 0L,
    val appsSizeBytes: Long = 0L,
    val downloadsSizeBytes: Long = 0L,

    /**
     * System OS, Firmware, reserved partitions, and unclassified files.
     * Calculated as: usedBytes - (photos + videos + screenshots + apps + downloads)
     */
    val systemAndOtherSizeBytes: Long = 0L
) {
    /** Used space as a fraction of total, in `[0.0, 1.0]`. Safe against a zero-size volume. */
    val usedFraction: Float
        get() = if (totalBytes <= 0L) 0f else (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

    // Base-10 Display values (Commercial standard used by phone manufacturers: 1 GB = 1,000,000,000 Bytes)
    val totalGb: Float get() = totalBytes / BYTES_IN_GB_COMMERCIAL
    val usedGb: Float get() = usedBytes / BYTES_IN_GB_COMMERCIAL
    val freeGb: Float get() = freeBytes / BYTES_IN_GB_COMMERCIAL

    val systemAndOtherGb: Float get() = systemAndOtherSizeBytes / BYTES_IN_GB_COMMERCIAL

    companion object {
        /** Standard Base-10 conversion for commercial storage display */
        const val BYTES_IN_GB_COMMERCIAL = 1_000_000_000f

        /** Base-2 conversion if needed for precise binary calculations (GiB) */
        const val BYTES_IN_GIB_BINARY = 1024f * 1024f * 1024f

        /**
         * Round up raw hardware bytes to the nearest standard commercial phone storage size.
         * Example: 118,000,000,000 Bytes (~118GB raw) -> 128,000,000,000 Bytes (128GB Commercial)
         */
        fun roundToCommercialTotalBytes(rawTotalBytes: Long): Long {
            val rawGb = rawTotalBytes / BYTES_IN_GB_COMMERCIAL
            val commercialGb = when {
                rawGb <= 16f -> 16L
                rawGb <= 32f -> 32L
                rawGb <= 64f -> 64L
                rawGb <= 128f -> 128L
                rawGb <= 256f -> 256L
                rawGb <= 512f -> 512L
                rawGb <= 1024f -> 1024L
                else -> (kotlin.math.ceil(rawGb / 128f) * 128).toLong() // Fallback for larger/unusual sizes
            }
            return commercialGb * 1_000_000_000L
        }
    }
}