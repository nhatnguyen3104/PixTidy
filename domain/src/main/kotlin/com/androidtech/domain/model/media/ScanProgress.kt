package com.androidtech.domain.model.media

/**
 * Point-in-time progress snapshot for a running scan, suitable for direct binding to a progress UI.
 */
data class ScanProgress(
    val currentLayer: ScanLayer,
    val processedItems: Int,
    val totalItems: Int,
    val currentStepMessage: String,
    val progressPercentage: Float
) {
    init {
        require(progressPercentage in 0f..100f) {
            "progressPercentage must be within 0..100, was $progressPercentage"
        }
    }

    companion object {
        fun of(layer: ScanLayer, processed: Int, total: Int, message: String): ScanProgress {
            val safeTotal = total.coerceAtLeast(1)
            val pct = (processed.toFloat() / safeTotal.toFloat() * 100f).coerceIn(0f, 100f)
            return ScanProgress(layer, processed, total, message, pct)
        }
    }
}