package com.androidtech.domain.repository

import com.androidtech.domain.model.media.StorageStats
import kotlinx.coroutines.flow.Flow

/**
 * Source of device storage capacity/usage information.
 *
 * Implementations live in the data layer (e.g. backed by [android.os.StatFs]
 * and MediaStore aggregate queries) and are responsible for translating
 * platform APIs into the pure [StorageStats] model.
 */
interface StorageRepository {

    /**
     * A continuously-updating stream of storage statistics.
     *
     * Implementations should emit an initial value promptly and re-emit
     * whenever the underlying usage materially changes (e.g. after a delete
     * operation, or on a polling/observer interval). This is a "hot" view of
     * current state, not a one-shot request/response.
     */
    fun observeStorageStats(): Flow<StorageStats>

    /**
     * A single, immediate snapshot of storage statistics.
     *
     * Prefer [observeStorageStats] for UI that should stay live; use this for
     * one-off checks (e.g. "is there enough free space before this operation").
     */
    suspend fun getStorageStatsSnapshot(): StorageStats
}