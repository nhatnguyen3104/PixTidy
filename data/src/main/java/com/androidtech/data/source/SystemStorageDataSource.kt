package com.androidtech.data.source

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads raw device-level storage capacity/usage figures.
 *
 * Prefers [StorageStatsManager] (API 26+), which reports accurate figures respecting
 * the OS's own accounting (including cache reclamation), and falls back to [StatFs]
 * for older API levels.
 */
class SystemStorageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Raw total/free bytes for the primary shared storage volume. Category breakdown (photos,
     * videos, screenshots) is intentionally left to the repository layer, which composes this
     * with [com.androidtech.domain.repository.MediaRepository] data. */
    suspend fun getTotalAndFreeBytes(): Pair<Long, Long> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val statsManager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val uuid = storageManager.getUuidForPath(Environment.getDataDirectory())
                val total = statsManager.getTotalBytes(uuid)
                val free = statsManager.getFreeBytes(uuid)
                return@withContext total to free
            } catch (e: Exception) {
                // Fall through to StatFs on any platform quirk (e.g. missing volume UUID).
            }
        }
        statFsFallback()
    }

    private fun statFsFallback(): Pair<Long, Long> {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.blockCountLong * stat.blockSizeLong
        val free = stat.availableBlocksLong * stat.blockSizeLong
        return total to free
    }
}