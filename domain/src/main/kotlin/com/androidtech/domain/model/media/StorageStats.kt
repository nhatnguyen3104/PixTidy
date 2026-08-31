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
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val photosSizeBytes: Long = 0L,
    val videosSizeBytes: Long = 0L,
    val screenshotsSizeBytes: Long = 0L,
    val duplicatesSizeBytes: Long = 0L,
    val otherSizeBytes: Long = 0L
) {
    /** Used space as a fraction of total, in `[0.0, 1.0]`. Safe against a zero-size volume. */
    val usedFraction: Float
        get() = if (totalBytes <= 0L) 0f else (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)

    val usedGb: Float get() = usedBytes / BYTES_IN_GB
    val totalGb: Float get() = totalBytes / BYTES_IN_GB
    val freeGb: Float get() = freeBytes / BYTES_IN_GB

}

private const val BYTES_IN_GB = 1024f * 1024f * 1024f