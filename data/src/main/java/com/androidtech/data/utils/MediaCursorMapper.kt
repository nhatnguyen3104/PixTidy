package com.androidtech.data.utils

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.androidtech.domain.model.media.MediaItem

/**
 * Maps MediaStore cursor rows to the pure-Kotlin [MediaItem] domain model. Kept as a standalone
 * object (rather than inlined into the repository) so it's independently testable against a
 * MatrixCursor without standing up a full ContentResolver.
 */
internal object MediaCursorMapper {

    /** Column set shared by both the Fast Scan projection and the Smart Scan candidate projection. */
    val BASE_PROJECTION = arrayOf(
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

    val VIDEO_PROJECTION = BASE_PROJECTION + MediaStore.Video.VideoColumns.DURATION

    fun mapImageRow(cursor: Cursor, collectionUri: Uri): MediaItem =
        mapRow(cursor, collectionUri, isVideo = false)

    fun mapVideoRow(cursor: Cursor, collectionUri: Uri): MediaItem =
        mapRow(cursor, collectionUri, isVideo = true)

    private fun mapRow(cursor: Cursor, collectionUri: Uri, isVideo: Boolean): MediaItem {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
        val contentUri = ContentUris.withAppendedId(collectionUri, id)

        val duration = if (isVideo) {
            val durationIndex = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
            if (durationIndex >= 0) cursor.getLong(durationIndex) else null
        } else {
            null
        }

        return MediaItem(
            id = id,
            uri = contentUri.toString(),
            path = cursor.getStringOrEmpty(MediaStore.MediaColumns.DATA),
            name = cursor.getStringOrEmpty(MediaStore.MediaColumns.DISPLAY_NAME),
            size = cursor.getLongOrZero(MediaStore.MediaColumns.SIZE),
            mimeType = cursor.getStringOrEmpty(MediaStore.MediaColumns.MIME_TYPE),
            dateAdded = cursor.getLongOrZero(MediaStore.MediaColumns.DATE_ADDED) * 1000L, // MediaStore stores seconds
            dateModified = cursor.getLongOrZero(MediaStore.MediaColumns.DATE_MODIFIED) * 1000L,
            width = cursor.getIntOrZero(MediaStore.MediaColumns.WIDTH),
            height = cursor.getIntOrZero(MediaStore.MediaColumns.HEIGHT),
            duration = duration
        )
    }

    private fun Cursor.getStringOrEmpty(column: String): String {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getString(index).orEmpty() else ""
    }

    private fun Cursor.getLongOrZero(column: String): Long {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getLong(index) else 0L
    }

    private fun Cursor.getIntOrZero(column: String): Int {
        val index = getColumnIndex(column)
        return if (index >= 0 && !isNull(index)) getInt(index) else 0
    }
}
