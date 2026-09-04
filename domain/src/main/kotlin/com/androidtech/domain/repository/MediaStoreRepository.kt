package com.androidtech.domain.repository

import com.androidtech.domain.model.media.MediaItem
import kotlinx.coroutines.flow.Flow

/** Abstraction over device media storage. Implemented in the data layer against MediaStore. */
interface MediaStoreRepository {

    /**
     * Streams all images and videos in fixed-size batches (see implementation for batch size),
     * projecting only the lightweight columns needed for Fast Scan. Each emission is a fresh
     * batch, not a running accumulation — accumulation is a use-case concern.
     */
    fun fetchAllMediaFast(): Flow<List<MediaItem>>

    /**
     * Blocking (suspend) fetch of the full candidate set for Smart Scan / Deep Smart Scan —
     * images and videos with valid dimensions, excluding thumbnails/icons below a sane minimum
     * size. Marked suspend (rather than the plain synchronous signature) because it performs a
     * blocking ContentResolver query; the implementation dispatches this to Dispatchers.IO.
     */
    suspend fun fetchCandidatesForSmartScan(): List<MediaItem>

    /**
     * Deletes the given content URIs. On API 30+ this triggers a system confirmation via
     * MediaStore.createDeleteRequest and the Result reflects whether the user approved it; on
     * API 29 a RecoverableSecurityException is surfaced to the caller as a failure so the UI
     * layer can resolve it via IntentSender; below API 29, direct ContentResolver.delete is used.
     */
    suspend fun deleteItems(uris: List<String>): Result<Unit>
}
