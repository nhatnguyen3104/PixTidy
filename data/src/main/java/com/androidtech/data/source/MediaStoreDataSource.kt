package com.androidtech.data.source

import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Size
import com.androidtech.data.mapper.toMediaItem
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.MediaType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import androidx.core.net.toUri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Android-specific data source for reading photo/video metadata via [ContentResolver]
 * against `MediaStore.Images` and `MediaStore.Video`, correctly scoped for Android 10+
 * (API 29+) scoped storage: only `_ID`, `DISPLAY_NAME`, `SIZE`, and similar metadata
 * columns are queried — no direct filesystem access is required for reads.
 */
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Reactive stream of all images; re-queries whenever the images collection changes. */
    fun observeImages(): Flow<List<MediaItem>> =
        observeCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false)

    /** Reactive stream of all videos; re-queries whenever the video collection changes. */
    fun observeVideos(): Flow<List<MediaItem>> =
        observeCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)

    /** One-shot combined query of every image and video known to MediaStore. */
    suspend fun queryAll(): List<MediaItem> = withContext(Dispatchers.IO) {
        queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false) +
                queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)
    }

    /**
     * Loads a memory-safe, downscaled thumbnail for [item] — never decodes the full-resolution
     * original into memory. Uses the platform thumbnail API on API 29+ and falls back to
     * `inSampleSize` subsampled decoding on older devices.
     */
    suspend fun loadThumbnail(item: MediaItem, targetSizePx: Int = 256): Bitmap? =
        withContext(Dispatchers.IO) {
            val uri = item.uriString.toUri()
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri,
                        Size(targetSizePx, targetSizePx), null)
                } else {
                    loadDownsampledBitmap(uri, targetSizePx)
                }
            } catch (e: Exception) {
                // Missing file, revoked permission, or corrupt media — treat as unavailable.
                null
            }
        }

    private fun observeCollection(collectionUri: Uri, isVideo: Boolean): Flow<List<MediaItem>> = callbackFlow {
        val resolver = context.contentResolver

        suspend fun emitCurrent() {
            trySend(withContext(Dispatchers.IO) { queryCollection(collectionUri, isVideo) })
        }

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                // Fire-and-forget re-query on any change notification from the provider,
                // launched on this callbackFlow's own producer scope (not GlobalScope) so it
                // is automatically cancelled when the Flow collector goes away.
                launch(Dispatchers.IO) {
                    trySend(queryCollection(collectionUri, isVideo))
                }
            }
        }

        resolver.registerContentObserver(collectionUri, /* notifyForDescendants = */ true, observer)
        emitCurrent()

        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    /**
     * Pages through [collectionUri] in batches of [pageSize], mapping each row via
     * [toMediaItem]. Paginating avoids holding an unbounded cursor open for devices
     * with very large media libraries.
     */
    private fun queryCollection(
        collectionUri: Uri,
        isVideo: Boolean,
        pageSize: Int = 500
    ): List<MediaItem> {
        val resolver = context.contentResolver
        val projection = buildProjection(isVideo)
        val results = mutableListOf<MediaItem>()

        try {
            var offset = 0
            while (true) {
                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, pageSize)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(MediaStore.MediaColumns.DATE_ADDED)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                }

                val pageCount = resolver.query(collectionUri, projection, queryArgs, null)?.use { cursor ->
                    while (cursor.moveToNext()) {
                        cursor.toMediaItem(collectionUri, isVideo)?.let { results.add(it) }
                    }
                    cursor.count
                } ?: 0

                if (pageCount < pageSize) break
                offset += pageSize
            }
        } catch (e: SecurityException) {
            // Storage/media permission not granted or revoked mid-scan; return whatever
            // was collected before the failure rather than throwing through Flow.
        }

        return results
    }

    private fun buildProjection(isVideo: Boolean): Array<String> {
        val base = arrayOf(
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
        return if (isVideo) base + MediaStore.Video.VideoColumns.DURATION else base
    }

    /** Pre-API-29 fallback: decode bounds first, compute subsampling, then decode at reduced size. */
    private fun loadDownsampledBitmap(uri: Uri, targetSizePx: Int): Bitmap? {
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null

        val sampleSize = calculateInSampleSize(bounds, targetSizePx, targetSizePx)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
