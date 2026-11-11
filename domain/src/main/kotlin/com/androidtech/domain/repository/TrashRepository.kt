package com.androidtech.domain.repository

import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.TrashActionResult
import kotlinx.coroutines.flow.Flow


/**
 * Access to the system trash bin (items the user deleted but that can still
 * be restored until the platform expires them).
 *
 * Mutating operations take [MediaItem.id]s and report back through
 * [TrashActionResult] rather than throwing, because "the user must confirm
 * this first" is an expected outcome, not a failure.
 */
interface TrashRepository {

    /** Every item currently in the trash, most recently modified first. */
    fun getTrashedItems(): Flow<List<MediaItem>>

    /** Moves the given items to the trash. */
    suspend fun moveToTrash(itemIds: List<Long>): TrashActionResult

    /** Restores the given trashed items to their original location. */
    suspend fun restoreFromTrash(itemIds: List<Long>): TrashActionResult

    /** Permanently deletes the given items. This cannot be undone. */
    suspend fun deletePermanently(itemIds: List<Long>): TrashActionResult
}