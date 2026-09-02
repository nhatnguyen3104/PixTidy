package com.androidtech.domain.repository

import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.StorageStats
import kotlinx.coroutines.flow.Flow

/**
 * Provides device-level storage statistics and destructive operations on media.
 * Implemented by the data layer using Android's `StorageStatsManager` and `MediaStore`.
 */
interface StorageRepository {

    /** Continuous stream of storage statistics; re-emits as usage changes. */
    fun getStorageStats(): Flow<StorageStats>

    /**
     * Attempts to delete the given [items] from device storage.
     *
     * On Android 11+ this may require surfacing a system consent dialog (via
     * `MediaStore.createDeleteRequest`); implementations signal that need through
     * a failed [Result] carrying a consent-required exception, since a suspend
     * function cannot itself launch UI.
     *
     * @return [Result.success] with the count of items actually deleted, or
     *   [Result.failure] with the triggering exception (e.g. [SecurityException]).
     */
    suspend fun deleteMediaItems(items: List<MediaItem>): Result<Int>
}