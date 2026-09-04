package com.androidtech.domain.model.media

/** Overall lifecycle state emitted by the Smart Scan / Deep Smart Scan pipelines. */
sealed interface ScanState {

    data object Idle : ScanState

    data class Scanning(val progress: ScanProgress) : ScanState

    data class Success(
        val insights: List<SmartInsight>,
        val totalReclaimableBytes: Long
    ) : ScanState

    data class Error(val throwable: Throwable) : ScanState
}