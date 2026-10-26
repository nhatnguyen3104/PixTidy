package com.androidtech.data.repository

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.content.Context
import android.content.IntentSender
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.androidtech.data.logger.DataLogger
import com.androidtech.data.utils.MediaCursorMapper
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.repository.MediaStoreRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * MediaStore-backed [MediaStoreRepository]. All querying happens on [Dispatchers.IO]; cursor
 * results are copied into [MediaItem] and the [Cursor] is closed via `.use {}` before returning,
 * so no cursor ever escapes this class.
 */
class MediaStoreRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaStoreRepository {

    private val resolver: ContentResolver get() = context.contentResolver

    override fun fetchAllMediaFast(): Flow<List<MediaItem>> = flow {
        DataLogger.d("fetchAllMediaFast: starting image query")
        emitAll(queryBatched(IMAGES_COLLECTION, isVideo = false))
        DataLogger.d("fetchAllMediaFast: starting video query")
        emitAll(queryBatched(VIDEO_COLLECTION, isVideo = true))
    }.flowOn(Dispatchers.IO)

    override suspend fun fetchCandidatesForSmartScan(): List<MediaItem> = withContext(Dispatchers.IO) {
        val images = queryAll(IMAGES_COLLECTION, isVideo = false)
        val videos = queryAll(VIDEO_COLLECTION, isVideo = true)
        (images + videos).filter { candidate ->
            candidate.hasValidDimensions &&
                    (candidate.width >= MIN_CANDIDATE_DIMENSION_PX || candidate.height >= MIN_CANDIDATE_DIMENSION_PX)
        }
    }

    override suspend fun fetchTotalMediaSizeBytes(): Long = withContext(Dispatchers.IO) {
        sumSizeColumn(IMAGES_COLLECTION) + sumSizeColumn(VIDEO_COLLECTION)
    }

    /** Single-column pass (`SIZE` only) — cheap even on large libraries, no [MediaItem] mapping. */
    private fun sumSizeColumn(collectionUri: Uri): Long {
        var total = 0L
        resolver.query(
            collectionUri,
            arrayOf(MediaStore.MediaColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            if (sizeIndex < 0) return@use
            while (cursor.moveToNext()) {
                if (!cursor.isNull(sizeIndex)) total += cursor.getLong(sizeIndex)
            }
        }
        return total
    }

    override suspend fun deleteItems(uris: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            DataLogger.d("deleteItems: attempting to delete ${uris.size} items")
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> deleteApi30Plus(uris)
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> deleteApi29(uris)
                else -> deleteLegacy(uris)
            }
            DataLogger.d("deleteItems: successfully deleted all items")
            Result.success(Unit)
        } catch (ce: CancellationException) {
            DataLogger.d("deleteItems: cancelled")
            throw ce
        } catch (e: RequiresUserConfirmationException) {
            DataLogger.d("deleteItems: requires user confirmation")
            Result.failure(e)
        } catch (e: Exception) {
            DataLogger.e(e, "deleteItems: failed to delete items")
            Result.failure(e)
        }
    }

    // ---- Query helpers -----------------------------------------------------------------------

    /** Streams the collection in fixed-size batches, emitting a partial batch at end-of-cursor. */
    private fun queryBatched(collectionUri: Uri, isVideo: Boolean): Flow<List<MediaItem>> = flow {
        val projection = if (isVideo) MediaCursorMapper.VIDEO_PROJECTION else MediaCursorMapper.BASE_PROJECTION
        val buffer = ArrayList<MediaItem>(QUERY_BATCH_SIZE)
        var totalFound = 0

        resolver.query(
            collectionUri,
            projection,
            null,
            null,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
        )?.use { cursor ->
            DataLogger.d("queryBatched: cursor count = ${cursor.count} for isVideo=$isVideo")
            while (cursor.moveToNext()) {
                currentCoroutineContext().ensureActive()
                buffer += mapRow(cursor, collectionUri, isVideo)
                totalFound++
                if (buffer.size >= QUERY_BATCH_SIZE) {
                    DataLogger.d("queryBatched: emitting batch of ${buffer.size} items (total=$totalFound)")
                    emit(ArrayList(buffer))
                    buffer.clear()
                }
            }
        } ?: DataLogger.w("queryBatched: resolver.query returned null for $collectionUri")

        if (buffer.isNotEmpty()) {
            DataLogger.d("queryBatched: emitting final batch of ${buffer.size} items (total=$totalFound)")
            emit(buffer)
        }
        DataLogger.d("queryBatched: finished query for isVideo=$isVideo, totalFound=$totalFound")
    }

    private fun queryAll(collectionUri: Uri, isVideo: Boolean): List<MediaItem> {
        val projection = if (isVideo) MediaCursorMapper.VIDEO_PROJECTION else MediaCursorMapper.BASE_PROJECTION
        val results = ArrayList<MediaItem>()

        resolver.query(collectionUri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                results += mapRow(cursor, collectionUri, isVideo)
            }
        }
        return results
    }

    private fun mapRow(cursor: Cursor, collectionUri: Uri, isVideo: Boolean): MediaItem =
        if (isVideo) MediaCursorMapper.mapVideoRow(cursor, collectionUri)
        else MediaCursorMapper.mapImageRow(cursor, collectionUri)

    // ---- Delete flows, branched by API level -------------------------------------------------

    @RequiresApi(Build.VERSION_CODES.R)
    private fun deleteApi30Plus(uris: List<String>) {
        val contentUris = uris.map(Uri::parse)
        // On API 30+ this either deletes silently (items the app itself owns) or throws a
        // RecoverableSecurityException-equivalent via the returned PendingIntent that must be
        // launched by the UI layer for items owned by other apps. We always surface the
        // IntentSender to the caller rather than trying to guess ownership up front.
        val pendingIntent = MediaStore.createDeleteRequest(resolver, contentUris)
        throw RequiresUserConfirmationException(pendingIntent.intentSender)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun deleteApi29(uris: List<String>) {
        for (uriString in uris) {
            val uri = uriString.toUri()
            try {
                resolver.delete(uri, null, null)
            } catch (e: RecoverableSecurityException) {
                throw RequiresUserConfirmationException(e.userAction.actionIntent.intentSender)
            }
        }
    }

    private fun deleteLegacy(uris: List<String>) {
        val failures = mutableListOf<String>()
        for (uriString in uris) {
            val uri = uriString.toUri()
            val rowsDeleted = runCatching { resolver.delete(uri, null, null) }.getOrDefault(0)
            if (rowsDeleted <= 0) {
                // Legacy storage: MediaStore row may already be gone while the file remains, or
                // vice versa — fall back to direct file deletion using the path we have on record.
                val deletedFile = runCatching {
                    resolver.query(uri, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)
                        ?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val path = cursor.getString(0)
                                path != null && java.io.File(path).delete()
                            } else {
                                false
                            }
                        } ?: false
                }.getOrDefault(false)
                if (!deletedFile) failures += uriString
            }
        }
        if (failures.isNotEmpty()) {
            throw IllegalStateException("Failed to delete ${failures.size} of ${uris.size} item(s)")
        }
    }

    private companion object {
        val IMAGES_COLLECTION: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val VIDEO_COLLECTION: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        const val QUERY_BATCH_SIZE = 500
        const val MIN_CANDIDATE_DIMENSION_PX = 100 // excludes icons/stickers/thumbnails
    }
}


/**
 * Surfaced by [MediaStoreRepositoryImpl.deleteItems] on API 29+ when the platform requires an
 * interactive user confirmation before deletion can proceed (RecoverableSecurityException on
 * API 29, MediaStore.createDeleteRequest on API 30+).
 *
 * Deliberately lives in the data layer, not domain: [IntentSender] is an Android framework type,
 * and the domain interface's `Result<Unit>` return type lets this propagate as a normal failure.
 * The presentation layer (which already depends on Android) can catch this specific type via
 * `result.exceptionOrNull() as? RequiresUserConfirmationException` and launch [intentSender] with
 * `ActivityResultContracts.StartIntentSenderForResult`.
 */
class RequiresUserConfirmationException(
    val intentSender: IntentSender
) : Exception("Deletion requires explicit user confirmation via system dialog")
