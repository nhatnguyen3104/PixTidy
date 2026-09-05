package com.androidtech.domain.model.media

/**
 * System-wide storage snapshot for the volume holding app/user data (i.e. the internal/shared
 * storage volume returned by `Environment.getDataDirectory()`), plus how much of that is
 * attributable to scanned media specifically.
 */
data class StorageSummary(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val mediaUsedBytes: Long
) {
    init {
        require(totalBytes >= 0) { "totalBytes cannot be negative" }
        require(usedBytes >= 0) { "usedBytes cannot be negative" }
        require(freeBytes >= 0) { "freeBytes cannot be negative" }
        require(mediaUsedBytes >= 0) { "mediaUsedBytes cannot be negative" }
    }

    /** 0f..100f. Zero when [totalBytes] is zero (e.g. value not yet available) rather than NaN. */
    val usedPercentage: Float
        get() = if (totalBytes <= 0L) 0f else (usedBytes.toFloat() / totalBytes.toFloat() * 100f).coerceIn(0f, 100f)

    companion object {
        val UNKNOWN = StorageSummary(totalBytes = 0L, usedBytes = 0L, freeBytes = 0L, mediaUsedBytes = 0L)
    }
}
