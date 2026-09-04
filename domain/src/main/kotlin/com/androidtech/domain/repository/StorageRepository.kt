package com.androidtech.domain.repository

/** Abstraction for device storage information. */
interface StorageRepository {
    /** Returns current snapshot of device storage statistics. */
    fun getDeviceStorageStats(): DeviceStorageStats
}

data class DeviceStorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long
)
