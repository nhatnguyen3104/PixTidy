package com.androidtech.domain.repository

import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.MediaType
import kotlinx.coroutines.flow.Flow

/**
 * Provides read access to on-device media (photos, videos, screenshots).
 * Implemented by the data layer using `MediaStore` / `ContentResolver`.
 */
interface MediaRepository {

    /** Reactive stream of media items of the given [type]; re-emits when the media store changes. */
    fun getMediaItems(type: MediaType): Flow<List<MediaItem>>

    /** One-shot query returning every known media item (images + videos), for scan pipelines. */
    suspend fun queryAllMedia(): List<MediaItem>
}
