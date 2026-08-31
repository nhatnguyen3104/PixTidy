package com.androidtech.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import com.androidtech.data.logger.DataLogger
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.MediaType
import com.androidtech.domain.repository.MediaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import androidx.core.net.toUri

/**
 * Progress states for a full media-library scan (as opposed to duplicate
 * detection, which is [ScanProgress] in the
 * domain layer). Kept as a separate type deliberately: the domain
 * `ScanProgress.Completed` carries `duplicateGroups`, which has no meaning
 * for "I finished indexing the library" — reusing it here would force a
 * misleading empty list into a field that means something specific
 * elsewhere. If you'd rather unify the two, the domain model would need a
 * `Completed` variant generic over its payload.
 */
sealed interface MediaScanProgress {
    data class Scanning(
        val currentFile: String,
        val percentage: Int,
        val itemsFound: Int
    ) : MediaScanProgress

    data class Completed(val items: List<MediaItem>) : MediaScanProgress

    data class Error(val message: String, val cause: Throwable? = null) : MediaScanProgress
}

/**
 * Shared MediaStore selection fragments so screenshot-detection logic isn't
 * re-derived (and potentially drift out of sync) across repositories.
 */
internal object MediaStoreSelections {
    /** Matches both `Pictures/Screenshots/...` (RELATIVE_PATH) and the legacy `DCIM/Screenshots` bucket. */
    const val SCREENSHOT_SELECTION =
        "(${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} = ?)"
    val SCREENSHOT_SELECTION_ARGS = arrayOf("%Screenshots%", "Screenshots")

    // Not `const` because it's built from another const val via a template;
    // the compiler doesn't treat that as a compile-time constant expression.
    val NOT_SCREENSHOT_SELECTION = "NOT ($SCREENSHOT_SELECTION)"
}

/**
 * [MediaRepository] implementation querying [MediaStore] directly.
 *
 * Design notes:
 * - Every query uses a narrow, hand-picked projection (never `null`
 *   projection) so the ContentProvider doesn't marshal columns we don't use.
 * - Column indices are resolved once per cursor via [ColumnIndices], not
 *   per-row, which matters once libraries run into the tens of thousands of
 *   items.
 * - [MediaStore.MediaColumns.DATA] is still requested for compatibility
 *   (some internal tooling and legacy file-based flows expect a path), but
 *   under scoped storage it should be treated as informational only — don't
 *   do direct file I/O against it. Use [MediaItem.contentUri] instead.
 * - All blocking `ContentResolver` work runs on [ioDispatcher].
 */
class MediaRepositoryImpl(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MediaRepository {

    private val appContext = context.applicationContext
    private val resolver get() = appContext.contentResolver

    // region MediaRepository

    override fun observeMedia(types: Set<MediaType>): Flow<List<MediaItem>> {
        return callbackFlow {
            val handler = Handler(Looper.getMainLooper())
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    trySend(Unit)
                }
            }
            resolver.registerContentObserver(IMAGES_URI, true, observer)
            resolver.registerContentObserver(VIDEO_URI, true, observer)
            awaitClose { resolver.unregisterContentObserver(observer) }
        }
            .onStart { emit(Unit) }
            .debounce(300L)
            .conflate()
            .map { getMedia(types) }
            .distinctUntilChanged()
            .flowOn(ioDispatcher)
    }

    override suspend fun getMedia(types: Set<MediaType>): List<MediaItem> =
        withContext(ioDispatcher) {
            if (types.isEmpty()) return@withContext emptyList()

            val result = mutableListOf<MediaItem>()

            val wantsImages = MediaType.IMAGE in types
            val wantsScreenshots = MediaType.SCREENSHOT in types
            if (wantsImages || wantsScreenshots) {
                result += queryImages(includeNonScreenshots = wantsImages, includeScreenshots = wantsScreenshots)
            }

            if (MediaType.VIDEO in types) {
                result += queryCollection(
                    collectionUri = VIDEO_URI,
                    selection = null,
                    selectionArgs = null,
                    sortOrder = null
                )
            }

            result
        }

    override suspend fun getMediaById(id: Long): MediaItem? = withContext(ioDispatcher) {
        queryById(IMAGES_URI, id) ?: queryById(VIDEO_URI, id)
    }

    override suspend fun deleteMedia(items: List<MediaItem>): List<Long> =
        withContext(ioDispatcher) {
            val deletedIds = mutableListOf<Long>()
            for (item in items) {
                currentCoroutineContext().ensureActive()
                try {
                    val uri = item.contentUri.toUri()
                    val rows = resolver.delete(uri, null, null)
                    if (rows > 0) deletedIds += item.id
                } catch (e: SecurityException) {
                    // Expected on API 29+ when the app doesn't own the file and the
                    // user hasn't granted a one-off deletion consent yet. Recovering
                    // from this requires launching a RecoverableSecurityException's
                    // IntentSender from an Activity, which is outside what a
                    // suspend-fun repository contract can express — surfacing that
                    // is a use-case/ViewModel-layer concern. We skip and continue so
                    // one un-deletable item doesn't abort the whole batch.
                    DataLogger.w(TAG, "Delete denied for id=${item.id}, needs user consent", e)
                } catch (e: Exception) {
                    DataLogger.e(TAG, "Delete failed for id=${item.id}", e)
                }
            }
            deletedIds
        }

    // endregion

    // region Extra capabilities (beyond the base MediaRepository contract)

    /**
     * Performs a full library scan as a **cold** [Flow]: nothing runs until a
     * collector subscribes, and every new collector re-runs the scan from
     * scratch. Emits [MediaScanProgress.Scanning] every [progressEveryNItems]
     * rows (default 100) to keep UI updates cheap and non-spammy on large
     * libraries, followed by exactly one terminal [MediaScanProgress.Completed]
     * or [MediaScanProgress.Error].
     *
     * Cancellation: the cursor is opened inside a `use { }` block, so if the
     * collecting coroutine is cancelled mid-scan, [ensureActive] throws at the
     * next row boundary, propagates as a normal flow cancellation, and the
     * cursor is still closed via `use`'s `finally`.
     */
    fun scanMediaLibrary(
        types: Set<MediaType> = setOf(MediaType.IMAGE, MediaType.VIDEO, MediaType.SCREENSHOT),
        progressEveryNItems: Int = 100
    ): Flow<MediaScanProgress> = flow {
        if (types.isEmpty()) {
            emit(MediaScanProgress.Completed(emptyList()))
            return@flow
        }

        val specs = buildQuerySpecs(types)
        val totalEstimate = specs.sumOf { spec ->
            resolver.query(spec.uri, arrayOf(MediaStore.MediaColumns._ID), spec.selection, spec.selectionArgs, null)
                ?.use { it.count } ?: 0
        }

        val collected = ArrayList<MediaItem>(totalEstimate.coerceAtLeast(0))
        var processed = 0

        for (spec in specs) {
            resolver.query(spec.uri, PROJECTION, spec.selection, spec.selectionArgs, spec.sortOrder)
                ?.use { cursor ->
                    val columns = ColumnIndices(cursor)
                    while (cursor.moveToNext()) {
                        currentCoroutineContext().ensureActive()

                        val item = cursor.toMediaItem(columns, spec.uri)
                        collected += item
                        processed++

                        if (processed % progressEveryNItems == 0) {
                            val percentage = if (totalEstimate > 0) {
                                ((processed * 100L) / totalEstimate).toInt().coerceIn(0, 99)
                            } else {
                                0
                            }
                            emit(
                                MediaScanProgress.Scanning(
                                    currentFile = item.displayName,
                                    percentage = percentage,
                                    itemsFound = processed
                                )
                            )
                        }
                    }
                }
        }

        emit(MediaScanProgress.Scanning(currentFile = "", percentage = 100, itemsFound = processed))
        emit(MediaScanProgress.Completed(collected))
    }
        .flowOn(ioDispatcher)
        .catch { throwable ->
            if (throwable is CancellationException) throw throwable
            DataLogger.e( "scanMediaLibrary failed", throwable)
            emit(MediaScanProgress.Error(throwable.message ?: "Unknown error scanning media", throwable))
        }

    /**
     * Videos at or above [minSizeBytes] (default 50MB), largest first. Sorting
     * is pushed down to the ContentProvider via `ORDER BY SIZE DESC` rather
     * than loaded-then-sorted client-side.
     */
    suspend fun getLargeVideos(minSizeBytes: Long = DEFAULT_LARGE_VIDEO_THRESHOLD_BYTES): List<MediaItem> =
        withContext(ioDispatcher) {
            queryCollection(
                collectionUri = VIDEO_URI,
                selection = "${MediaStore.MediaColumns.SIZE} > ?",
                selectionArgs = arrayOf(minSizeBytes.toString()),
                sortOrder = "${MediaStore.MediaColumns.SIZE} DESC"
            )
        }

    // endregion

    // region Query helpers

    private data class QuerySpec(
        val uri: Uri,
        val selection: String?,
        val selectionArgs: Array<String>?,
        val sortOrder: String? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as QuerySpec

            if (uri != other.uri) return false
            if (selection != other.selection) return false
            if (!selectionArgs.contentEquals(other.selectionArgs)) return false
            if (sortOrder != other.sortOrder) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uri.hashCode()
            result = 31 * result + (selection?.hashCode() ?: 0)
            result = 31 * result + (selectionArgs?.contentHashCode() ?: 0)
            result = 31 * result + (sortOrder?.hashCode() ?: 0)
            return result
        }
    }

    private fun buildQuerySpecs(types: Set<MediaType>): List<QuerySpec> {
        val specs = mutableListOf<QuerySpec>()
        val wantsImages = MediaType.IMAGE in types
        val wantsScreenshots = MediaType.SCREENSHOT in types

        when {
            wantsImages && wantsScreenshots -> specs += QuerySpec(IMAGES_URI, null, null)
            wantsImages -> specs += QuerySpec(
                IMAGES_URI,
                MediaStoreSelections.NOT_SCREENSHOT_SELECTION,
                MediaStoreSelections.SCREENSHOT_SELECTION_ARGS
            )
            wantsScreenshots -> specs += QuerySpec(
                IMAGES_URI,
                MediaStoreSelections.SCREENSHOT_SELECTION,
                MediaStoreSelections.SCREENSHOT_SELECTION_ARGS
            )
        }

        if (MediaType.VIDEO in types) {
            specs += QuerySpec(VIDEO_URI, null, null)
        }

        return specs
    }

    private fun queryImages(includeNonScreenshots: Boolean, includeScreenshots: Boolean): List<MediaItem> {
        val (selection, args) = when {
            includeNonScreenshots && includeScreenshots -> null to null
            includeNonScreenshots -> MediaStoreSelections.NOT_SCREENSHOT_SELECTION to MediaStoreSelections.SCREENSHOT_SELECTION_ARGS
            includeScreenshots -> MediaStoreSelections.SCREENSHOT_SELECTION to MediaStoreSelections.SCREENSHOT_SELECTION_ARGS
            else -> return emptyList()
        }
        return queryCollection(IMAGES_URI, selection, args, sortOrder = null)
    }

    private fun queryCollection(
        collectionUri: Uri,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        resolver.query(collectionUri, PROJECTION, selection, selectionArgs, sortOrder)?.use { cursor ->
            val columns = ColumnIndices(cursor)
            while (cursor.moveToNext()) {
                items += cursor.toMediaItem(columns, collectionUri)
            }
        }
        return items
    }

    private fun queryById(collectionUri: Uri, id: Long): MediaItem? {
        val selection = "${MediaStore.MediaColumns._ID} = ?"
        val args = arrayOf(id.toString())
        resolver.query(collectionUri, PROJECTION, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.toMediaItem(ColumnIndices(cursor), collectionUri)
            }
        }
        return null
    }

    /** Resolves projection column indices once per cursor instead of per row. */
    private class ColumnIndices(cursor: Cursor) {
        val id = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
        val data = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
        val displayName = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
        val size = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
        val mimeType = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
        val width = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
        val height = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
        val duration = cursor.getColumnIndex(MediaStore.MediaColumns.DURATION)
    }

    private fun Cursor.toMediaItem(columns: ColumnIndices, collectionUri: Uri): MediaItem {
        val id = getLong(columns.id)
        val durationMs = if (columns.duration >= 0) {
            getLong(columns.duration).takeIf { collectionUri == VIDEO_URI }
        } else {
            null
        }
        return MediaItem(
            id = id,
            contentUri = ContentUris.withAppendedId(collectionUri, id).toString(),
            path = if (columns.data >= 0) getString(columns.data).orEmpty() else "",
            displayName = getString(columns.displayName).orEmpty(),
            sizeBytes = getLong(columns.size),
            mimeType = getString(columns.mimeType).orEmpty(),
            dateAddedEpochSec = getLong(columns.dateAdded),
            width = getInt(columns.width),
            height = getInt(columns.height),
            durationMs = durationMs
        )
    }

    // endregion

    private companion object {
        const val TAG = "MediaRepositoryImpl"
        const val DEFAULT_LARGE_VIDEO_THRESHOLD_BYTES = 50L * 1024 * 1024 // 50MB

        val IMAGES_URI: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val VIDEO_URI: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        /**
         * Narrow projection: only columns MediaItem actually needs. Avoids the
         * ContentProvider marshalling columns (e.g. ORIENTATION, DATE_MODIFIED,
         * various metadata blobs) that would otherwise be pulled across the
         * IPC boundary for every single row.
         */
        val PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION
        )
    }
}