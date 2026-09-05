package com.androidtech.domain.model.media

/** Overall lifecycle state emitted by the Smart Scan / Deep Smart Scan pipelines. */
sealed interface ScanProcess {

    data object Idle : ScanProcess

    data class Scanning(val progress: ScanProgress) : ScanProcess

    data class Success(
        val insights: List<SmartInsight>,
        val totalReclaimableBytes: Long
    ) : ScanProcess

    data class Error(val throwable: Throwable) : ScanProcess
}