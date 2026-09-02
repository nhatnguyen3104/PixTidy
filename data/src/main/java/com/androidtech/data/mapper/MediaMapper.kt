package com.androidtech.data.mapper

import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.MediaType

/**
 * Safely reads a nullable [String] column, since MediaStore rows can legitimately
 * contain NULLs for optional metadata (e.g. DATA path on some scoped-storage devices).
 */
internal fun Cursor.getStringOrNull(columnIndex: Int): String? =
    if (columnIndex < 0 || isNull(columnIndex)) null else getString(columnIndex)

internal fun Cursor.getIntOrNull(columnIndex: Int): Int? =
    if (columnIndex < 0 || isNull(columnIndex)) null else getInt(columnIndex)

internal fun Cursor.getLongOrNull(columnIndex: Int): Long? =
    if (columnIndex < 0 || isNull(columnIndex)) null else getLong(columnIndex)

/**
 * Maps the current row of a `MediaStore.Images` or `MediaStore.Video` [Cursor] — produced
 * by a query against [contentUri] with the projection from [MediaProjection.forCollection] —
 * into a domain [MediaItem].
 *
 * Returns null rather than throwing if required columns are missing or malformed, so a single
 * corrupt row can't crash an entire scan; the caller simply skips it.
 *
 * @param contentUri The base collection URI (e.g. `MediaStore.Images.Media.EXTERNAL_CONTENT_URI`)
 *   used to reconstruct a per-item content URI via [ContentUris.withAppendedId].
 * @param isVideo Whether this cursor was queried against the video collection, which changes
 *   which MIME/duration columns are available.
 */
fun Cursor.toMediaItem(contentUri: Uri, isVideo: Boolean): MediaItem? {
    return try {
        val idIndex = getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val id = getLong(idIndex)
        val itemUri = ContentUris.withAppendedId(contentUri, id)

        val displayNameIndex = getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val displayName = getStringOrNull(displayNameIndex) ?: return null

        val sizeIndex = getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val sizeBytes = getLongOrNull(sizeIndex) ?: 0L

        val mimeIndex = getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
        val mimeType = getStringOrNull(mimeIndex) ?: if (isVideo) "video/*" else "image/*"

        val dateAddedIndex = getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
        val dateAdded = getLongOrNull(dateAddedIndex) ?: 0L

        val dateModifiedIndex = getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
        val dateModified = getLongOrNull(dateModifiedIndex) ?: dateAdded

        val widthIndex = getColumnIndex(MediaStore.MediaColumns.WIDTH)
        val width = getIntOrNull(widthIndex) ?: 0

        val heightIndex = getColumnIndex(MediaStore.MediaColumns.HEIGHT)
        val height = getIntOrNull(heightIndex) ?: 0

        // DATA (filesystem path) is deprecated but still populated on most OEMs; treat as best-effort.
        val pathIndex = getColumnIndex(MediaStore.MediaColumns.DATA)
        val path = getStringOrNull(pathIndex) ?: ""

        val durationMs = if (isVideo) {
            val durationIndex = getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
            getLongOrNull(durationIndex)
        } else {
            null
        }

        MediaItem(
            id = id,
            uriString = itemUri.toString(),
            path = path,
            displayName = displayName,
            sizeBytes = sizeBytes,
            mimeType = mimeType,
            dateAddedEpochSec = dateAdded,
            dateModifiedEpochSec = dateModified,
            width = width,
            height = height,
            durationMs = durationMs
        )
    } catch (e: IllegalArgumentException) {
        // A required column was missing from the projection/cursor — skip this row.
        null
    }
}

/** Filename fragments used to heuristically identify screenshots across common OEM camera apps. */
private val SCREENSHOT_MARKERS = listOf("screenshot", "screen_shot", "screen shot")

/**
 * Classifies a [MediaItem] into a [MediaType] based on its MIME type and, for images,
 * a filename/path heuristic that flags screenshots (MediaStore has no reliable first-class
 * "is screenshot" flag across all Android versions and OEMs).
 */
fun MediaItem.inferMediaType(): MediaType {
    return when {
        mimeType.startsWith("video/") -> MediaType.VIDEO
        mimeType.startsWith("image/") -> {
            val haystack = "$displayName $path".lowercase()
            if (SCREENSHOT_MARKERS.any { haystack.contains(it) }) {
                MediaType.SCREENSHOT
            } else {
                MediaType.IMAGE
            }
        }
        else -> MediaType.UNKNOWN
    }
}
