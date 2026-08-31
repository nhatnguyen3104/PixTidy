package com.androidtech.domain.repository

import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.MediaType
import kotlinx.coroutines.flow.Flow

/**
 * Read access to the device's indexed media (photos, videos, screenshots).
 *
 * Implementations are expected to be backed by the platform media store, but
 * the domain layer only depends on this interface — no `ContentResolver`,
 * `Uri`, or other Android types leak through the contract.
 */
interface MediaRepository {

    /**
     * Observes all media items of the given [types] as the underlying store
     * changes (new photo taken, file deleted externally, etc).
     *
     * @param types Which [MediaType]s to include. An empty set yields an
     *   empty (but still live) stream, not "all types" — callers must be
     *   explicit.
     */
    fun observeMedia(types: Set<MediaType>): Flow<List<MediaItem>>

    /**
     * One-shot fetch of all media items of the given [types].
     *
     * @param types Which [MediaType]s to include.
     */
    suspend fun getMedia(types: Set<MediaType>): List<MediaItem>

    /**
     * Fetches a single item by id, or `null` if it no longer exists
     * (e.g. deleted since it was last indexed).
     */
    suspend fun getMediaById(id: Long): MediaItem?

    /**
     * Deletes the given items from device storage.
     *
     * @param items Items to delete.
     * @return The ids that were successfully deleted. On platforms requiring
     *   user confirmation for deletion (scoped storage), a partial result is
     *   expected and normal — callers should not treat a short list as an
     *   error by itself.
     */
    suspend fun deleteMedia(items: List<MediaItem>): List<Long>
}