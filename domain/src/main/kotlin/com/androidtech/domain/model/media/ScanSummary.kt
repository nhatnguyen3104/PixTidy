package com.androidtech.domain.model.media

import kotlin.math.round

/**
 * Lightweight, cumulative snapshot produced by scan pipelines.
 * Emitted repeatedly as MediaStore batches stream in, converging to the final totals.
 */
data class ScanSummary(
    val lastScanLevel: ScanLayer,
    val newItemsCount: Int = 0,
    val totalMediaCount: Int,
    val totalStorageBytes: Long,
    val screenshotCount: Int,
    val screenshotBytes: Long,
    val largeVideoCount: Int,
    val largeVideoBytes: Long,
    val photosSizeBytes: Long,
    val videosSizeBytes: Long,
    val duplicatesSizeBytes: Long,
    val storageSummary: StorageSummary
) {
    companion object {
        val EMPTY = ScanSummary(
            lastScanLevel = ScanLayer.NONE,
            newItemsCount = 0,
            totalMediaCount = 0,
            totalStorageBytes = 0L,
            screenshotCount = 0,
            screenshotBytes = 0L,
            largeVideoCount = 0,
            largeVideoBytes = 0L,
            photosSizeBytes = 0L,
            videosSizeBytes = 0L,
            duplicatesSizeBytes = 0L,
            storageSummary = StorageSummary.UNKNOWN
        )
    }

    val deviceTotalGb: Float get() = (round(storageSummary.totalBytes / 1_000_000_000.0 * 10) / 10).toFloat()
    val deviceUsedGb: Float get() = (round(storageSummary.usedBytes  / 1_000_000_000.0 * 10) / 10).toFloat()
    val deviceFreeGb: Float get() = (round(storageSummary.freeBytes  / 1_000_000_000.0 * 10) / 10).toFloat()
}
