package com.androidtech.data.repository

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.TrashActionResult
import com.androidtech.domain.repository.TrashRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [TrashRepository] backed by the MediaStore system trash (Android 11+ / API 30+).
 *
 * Moving to trash, restoring and permanent deletion are *requests*: MediaStore hands back a
 * [PendingIntent] for a system consent dialog, and the platform performs the change itself once
 * the user approves. That `PendingIntent` is wrapped in [TrashActionResult.RequiresUserConsent]
 * so the domain layer never sees an Android type; the presentation layer unwraps it.
 *
 * Only a [ContentResolver] is needed (no `Context`): it is all MediaStore requires.
 *
 * On API 29 there is no system trash, so reads return an empty list and mutations return
 * [TrashActionResult.Error] with an [UnsupportedOperationException].
 */
class TrashRepositoryImpl @Inject constructor (
    private val contentResolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TrashRepository {

    override fun getTrashedItems(): Flow<List<MediaItem>> = callbackFlow {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        trySend(queryTrashedItems())
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryTrashedItems())
            }
        }
        contentResolver.registerContentObserver(FILES_URI, true, observer)
        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }

    override suspend fun moveToTrash(itemIds: List<Long>): TrashActionResult =
        requestUserConsent(itemIds, ConsentAction.TRASH)

    override suspend fun restoreFromTrash(itemIds: List<Long>): TrashActionResult =
        requestUserConsent(itemIds, ConsentAction.RESTORE)

    override suspend fun deletePermanently(itemIds: List<Long>): TrashActionResult =
        requestUserConsent(itemIds, ConsentAction.DELETE)

    // region Consent requests

    private enum class ConsentAction { TRASH, RESTORE, DELETE }

    private suspend fun requestUserConsent(
        itemIds: List<Long>,
        action: ConsentAction
    ): TrashActionResult = withContext(ioDispatcher) {
        if (itemIds.isEmpty()) return@withContext TrashActionResult.Success
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@withContext TrashActionResult.Error(
                UnsupportedOperationException("The system trash requires Android 11 (API 30) or higher.")
            )
        }

        try {
            val uris = resolveUris(itemIds)
            if (uris.isEmpty()) {
                return@withContext TrashActionResult.Error(
                    NoSuchElementException("None of the requested items exist in MediaStore anymore.")
                )
            }
            TrashActionResult.RequiresUserConsent(createConsentRequest(action, uris))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            TrashActionResult.Error(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun createConsentRequest(action: ConsentAction, uris: List<Uri>): PendingIntent =
        when (action) {
            ConsentAction.TRASH -> MediaStore.createTrashRequest(contentResolver, uris, true)
            ConsentAction.RESTORE -> MediaStore.createTrashRequest(contentResolver, uris, false)
            ConsentAction.DELETE -> MediaStore.createDeleteRequest(contentResolver, uris)
        }

    /**
     * Turns domain ids back into typed MediaStore URIs (images and videos live in different
     * collections, so an id alone isn't enough). Trashed rows are hidden from queries by default,
     * hence [MediaStore.MATCH_INCLUDE]: restore and permanent delete target exactly those rows.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun resolveUris(itemIds: List<Long>): List<Uri> {
        val uris = ArrayList<Uri>(itemIds.size)
        for (chunk in itemIds.distinct().chunked(RESOLVE_CHUNK_SIZE)) {
            val args = Bundle().apply {
                putString(
                    ContentResolver.QUERY_ARG_SQL_SELECTION,
                    "${MediaStore.Files.FileColumns._ID} IN (${chunk.joinToString(",")})"
                )
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            }
            try {
                contentResolver.query(
                    FILES_URI,
                    arrayOf(MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE),
                    args,
                    null
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                    val typeIdx = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    while (cursor.moveToNext()) {
                        if (idIdx >= 0 && typeIdx >= 0) {
                            val isVideo = cursor.getInt(typeIdx) == MEDIA_TYPE_VIDEO
                            uris += typedUri(cursor.getLong(idIdx), isVideo)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return uris
    }

    // endregion

    // region Trash query

    @RequiresApi(Build.VERSION_CODES.R)
    private fun queryTrashedItems(): List<MediaItem> {
        val items = ArrayList<MediaItem>()
        try {
            items += queryTrashedCollection(
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                isVideo = false
            )
            items += queryTrashedCollection(
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                isVideo = true
            )
            items.sortByDescending { it.dateModified }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun queryTrashedCollection(
        uri: Uri,
        isVideo: Boolean
    ): List<MediaItem> {
        val items = ArrayList<MediaItem>()
        val projection = if (isVideo) {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT,
                MediaStore.Video.VideoColumns.DURATION
            )
        } else {
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT
            )
        }

        val bundle = Bundle().apply {
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.MediaColumns.DATE_MODIFIED)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
        }

        var cursor: Cursor? = try {
            contentResolver.query(uri, projection, bundle, null)
        } catch (_: Exception) {
            null
        }

        if (cursor == null || cursor.count == 0) {
            cursor?.close()
            cursor = try {
                contentResolver.query(
                    uri,
                    projection,
                    "${MediaStore.MediaColumns.IS_TRASHED} = 1",
                    null,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
                )
            } catch (_: Exception) {
                null
            }
        }

        cursor?.use { c ->
            val idIdx = c.getColumnIndex(MediaStore.MediaColumns._ID)
            val dataIdx = c.getColumnIndex(MediaStore.MediaColumns.DATA)
            val nameIdx = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIdx = c.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val mimeIdx = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val addedIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val modifiedIdx = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val widthIdx = c.getColumnIndex(MediaStore.MediaColumns.WIDTH)
            val heightIdx = c.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
            val durationIdx = if (isVideo) c.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1

            while (c.moveToNext()) {
                val id = if (idIdx >= 0) c.getLong(idIdx) else continue
                val itemUri = typedUri(id, isVideo)
                val rawName = if (nameIdx >= 0 && !c.isNull(nameIdx)) c.getString(nameIdx).orEmpty() else ""
                val cleanName = rawName.replace(TRASHED_PREFIX, "")
                val path = if (dataIdx >= 0 && !c.isNull(dataIdx)) c.getString(dataIdx).orEmpty() else ""
                val size = if (sizeIdx >= 0 && !c.isNull(sizeIdx)) c.getLong(sizeIdx) else 0L
                val mimeType = if (mimeIdx >= 0 && !c.isNull(mimeIdx)) c.getString(mimeIdx).orEmpty() else ""
                val dateAdded = if (addedIdx >= 0 && !c.isNull(addedIdx)) c.getLong(addedIdx) * 1000L else 0L
                val dateModified = if (modifiedIdx >= 0 && !c.isNull(modifiedIdx)) c.getLong(modifiedIdx) * 1000L else 0L
                val width = if (widthIdx >= 0 && !c.isNull(widthIdx)) c.getInt(widthIdx) else 0
                val height = if (heightIdx >= 0 && !c.isNull(heightIdx)) c.getInt(heightIdx) else 0
                val duration = if (durationIdx >= 0 && !c.isNull(durationIdx)) c.getLong(durationIdx) else null

                items += MediaItem(
                    id = id,
                    uri = itemUri.toString(),
                    path = path,
                    name = cleanName,
                    size = size,
                    mimeType = mimeType,
                    dateAdded = dateAdded,
                    dateModified = dateModified,
                    width = width,
                    height = height,
                    duration = duration,
                    isTrashed = true
                )
            }
        }
        return items
    }

    // endregion

    private fun typedUri(id: Long, isVideo: Boolean): Uri {
        val baseUri = ContentUris.withAppendedId(
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            id
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            baseUri.buildUpon().appendQueryParameter("includeTrashed", "1").build()
        } else {
            baseUri
        }
    }

    private companion object {
        const val RESOLVE_CHUNK_SIZE = 500
        const val MEDIA_TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        val TRASHED_PREFIX = Regex("^\\.trashed-\\d+-")
        val FILES_URI: Uri = MediaStore.Files.getContentUri("external")
    }
}
