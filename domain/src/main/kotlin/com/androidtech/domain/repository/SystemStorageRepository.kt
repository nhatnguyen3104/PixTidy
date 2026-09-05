package com.androidtech.domain.repository

import com.androidtech.domain.model.media.StorageSummary

/**
 * Abstraction over device-wide (not media-specific) storage statistics. Implemented in the data
 * layer against StatFs / StorageStatsManager.
 */
interface SystemStorageRepository {

    /**
     * Returns total/used/free bytes for the volume holding app and user data. [StorageSummary.mediaUsedBytes]
     * is always 0 here — this repository has no notion of media; the use case that combines this
     * with [MediaStoreRepository] fills that field in.
     */
    suspend fun fetchDeviceStorage(): StorageSummary
}
