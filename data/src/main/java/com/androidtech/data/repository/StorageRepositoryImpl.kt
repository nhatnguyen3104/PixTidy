package com.androidtech.data.repository

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.androidtech.data.logger.DataLogger
import com.androidtech.domain.repository.DeviceStorageStats
import com.androidtech.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StorageRepository {

    override fun getDeviceStorageStats(): DeviceStorageStats {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        var totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize

        // Try to get a more accurate total bytes using StorageStatsManager (API 26+)
        try {
            val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val totalBytesFromStats = storageStatsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
            if (totalBytesFromStats > totalBytes) {
                DataLogger.d("getDeviceStorageStats: updated totalBytes from $totalBytes to $totalBytesFromStats using StorageStatsManager")
                totalBytes = totalBytesFromStats
            }
        } catch (e: Exception) {
            DataLogger.e(e, "getDeviceStorageStats: failed to get total bytes from StorageStatsManager")
        }

        // Recalculate usedBytes based on the potentially updated totalBytes
        // This ensures Used + Free = Total (32GB), accounting for System OS space.
        val usedBytes = totalBytes - freeBytes

        DataLogger.d("getDeviceStorageStats: final total=$totalBytes, used=$usedBytes, free=$freeBytes")

        return DeviceStorageStats(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            freeBytes = freeBytes,
        )
    }
}
