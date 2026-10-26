package com.androidtech.data.datasource

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.annotation.RequiresApi
import com.androidtech.data.logger.DataLogger
import com.androidtech.domain.model.media.StorageSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads raw device storage totals. Prefers [StorageStatsManager] on API 26+ (queries the actual
 * storage volume UUID, which can be a more accurate figure than [StatFs] on devices with
 * multi-partition or adoptable storage setups) and falls back to [StatFs] everywhere else —
 * matching the spec's formulas exactly:
 *   totalBytes = blockCountLong * blockSizeLong
 *   freeBytes  = availableBlocksLong * blockSizeLong
 *   usedBytes  = totalBytes - freeBytes
 */
class SystemStorageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun readDeviceStorage(): StorageSummary {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            readViaStorageStatsManager() ?: readViaStatFs()
        } else {
            readViaStatFs()
        }
        return result ?: StorageSummary.UNKNOWN
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun readViaStorageStatsManager(): StorageSummary? = try {
        val storageStatsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
        val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        val volumeUuid = storageManager?.let { StorageManager.UUID_DEFAULT }

        if (storageStatsManager == null || volumeUuid == null) {
            null
        } else {
            val total = storageStatsManager.getTotalBytes(volumeUuid)
            val free = storageStatsManager.getFreeBytes(volumeUuid)
            val used = (total - free).coerceAtLeast(0L)
            DataLogger.d("getDeviceStorageStatsManager: final totalBytes=$total, usedBytes=$used, freeBytes=$free")
            StorageSummary(totalBytes = total, usedBytes = used, freeBytes = free, mediaUsedBytes = 0L)
        }
    } catch (e: Exception) {
        // Can throw IOException on some OEM ROMs for unusual volume states — StatFs is the safe fallback.
        null
    }

    private fun readViaStatFs(): StorageSummary? = try {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val total = statFs.blockCountLong * statFs.blockSizeLong
        val free = statFs.availableBlocksLong * statFs.blockSizeLong
        val used = (total - free).coerceAtLeast(0L)
        DataLogger.d("getDeviceStorageStats: final totalBytes=$total, usedBytes=$used, freeBytes=$free")
        StorageSummary(totalBytes = total, usedBytes = used, freeBytes = free, mediaUsedBytes = 0L)
    } catch (e: Exception) {
        null
    }
}
