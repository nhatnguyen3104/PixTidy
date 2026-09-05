package com.androidtech.domain.model.media

import kotlin.math.round

/**
 * Lightweight, cumulative snapshot produced by [com.androidtech.domain.usecase.RunFastScanUseCase].
 * Emitted repeatedly as MediaStore batches stream in, converging to the final totals — this is
 * what makes Fast Scan feel instant even on libraries with 50k+ items.
 *
 * [storageSummary] is system-wide (total/used/free device storage, not just media) and is fetched
 * once up front rather than accumulated per-batch — see [com.androidtech.domain.usecase.RunFastScanUseCase]
 * for why.
 */
data class FastScanSummary(
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
        val EMPTY = FastScanSummary(
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
