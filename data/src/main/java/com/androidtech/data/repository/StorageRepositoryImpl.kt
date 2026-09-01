package com.androidtech.data.repository

import android.app.usage.StorageStatsManager
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import com.androidtech.data.logger.DataLogger
import com.androidtech.domain.model.media.StorageStats
import com.androidtech.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Thrown when neither [StorageStatsManager]/[StorageManager] nor the
 * [Environment.getExternalStorageDirectory] fallback can produce a usable
 * storage reading (e.g. volume unmounted, path missing).
 */
class StorageUnavailableException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * [StorageRepository] implementation backed by Android's [StorageStatsManager]
 * (exact, OS-reported total/free/used space) and [MediaStore] (aggregate size
 * of photos/videos/screenshots).
 *
 * Storage volume handling: this implementation targets the device's primary
 * shared storage volume ([StorageManager.UUID_DEFAULT]), which is where
 * MediaStore-indexed photos/videos live on the overwhelming majority of
 * devices. Removable SD cards / adoptable storage are out of scope here and
 * would need a separate [StorageRepository] instance per [StorageVolume] if
 * required later.
 *
 * All suspend work runs on [ioDispatcher] (defaults to [Dispatchers.IO]) since
 * both the system stats calls and the MediaStore cursor queries are blocking
 * I/O.
 */
class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StorageRepository {

    private val appContext = context

    private val storageStatsManager: StorageStatsManager? by lazy {
        appContext.getSystemService(Context.STORAGE_STATS_SERVICE) as? StorageStatsManager
    }

    private val storageManager: StorageManager? by lazy {
        appContext.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
    }

    override fun observeStorageStats(): Flow<StorageStats> {
        // MediaStore has no dedicated "storage changed" broadcast, but any
        // insert/update/delete against the shared media collections is a
        // reasonable proxy for "the numbers we report might be stale now".
        // We watch both the images and video collections and recompute a
        // fresh snapshot on change, debounced so a burst of writes (e.g. a
        // multi-file delete) only triggers one recomputation.
        return callbackFlow {
            val handler = Handler(Looper.getMainLooper())
            val observer = object : ContentObserver(handler) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    trySend(Unit)
                }
            }

            val resolver = appContext.contentResolver
            resolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                /* notifyForDescendants = */ true,
                observer
            )
            resolver.registerContentObserver(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                /* notifyForDescendants = */ true,
                observer
            )

            awaitClose { resolver.unregisterContentObserver(observer) }
        }
            .onStart { emit(Unit) } // ensure an immediate first emission
            .debounce(300L.milliseconds)
            .conflate()
            .map { getStorageStatsSnapshot() }
            .distinctUntilChanged()
            .catch { throwable ->
                // Caught upstream of flowOn so the (blocking) fallback
                // computation still executes on ioDispatcher, not on
                // whatever dispatcher the collector happens to be on.
                DataLogger.e( "observeStorageStats failed, emitting fallback snapshot", throwable)
                emit(computeFallbackStats(throwable))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getStorageStatsSnapshot(): StorageStats = withContext(ioDispatcher) {
        try {
            computeSystemBackedStats()
        } catch (e: SecurityException) {
            DataLogger.e( "SecurityException reading system storage stats, falling back", e)
            computeFallbackStats(e)
        } catch (e: IOException) {
            DataLogger.e( "IOException reading system storage stats, falling back", e)
            computeFallbackStats(e)
        } catch (e: StorageUnavailableException) {
            DataLogger.e( "Storage volume unavailable, falling back", e)
            computeFallbackStats(e)
        }
    }

    /**
     * Primary path: exact figures from [StorageStatsManager]/[StorageManager],
     * enriched with a MediaStore-derived category breakdown.
     *
     * @throws SecurityException if the caller lacks the required access.
     * @throws IOException if the storage volume can't be queried.
     * @throws StorageUnavailableException if the required system services
     *   aren't available on this device/build.
     */
    private fun computeSystemBackedStats(): StorageStats {
        val statsManager = storageStatsManager
            ?: throw StorageUnavailableException("StorageStatsManager unavailable")
        val volumeUuid: UUID = StorageManager.UUID_DEFAULT

        // 1. Get raw capacity from Android OS (Base-2 Bytes)
        val rawTotalBytes = statsManager.getTotalBytes(volumeUuid)
        val rawFreeBytes = statsManager.getFreeBytes(volumeUuid)

        // 2. Normalize Total Bytes to commercial figure (e.g., 128,000,000,000 Bytes)
        val commercialTotalBytes = StorageStats.roundToCommercialTotalBytes(rawTotalBytes)

        // 3. Keep actual Free Bytes (as this is the space truly available to the user)
        val actualFreeBytes = rawFreeBytes

        // 4. Calculate new Used Bytes = Commercial Total - Actual Free
        // Any discrepancy (OS overhead + conversion rounding) is automatically bundled into usedBytes
        val calculatedUsedBytes = (commercialTotalBytes - actualFreeBytes).coerceAtLeast(0L)

        // 5. Categorized User Data
        val photosSize = sumMediaSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val videosSize = sumMediaSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        val screenshotsSize = sumScreenshotSize()
        val appsSize = sumAppsSize(volumeUuid)
        val downloadsSize = sumDownloadsSize()

        // Avoid double-counting: screenshots are a subset of the images collection
        val adjustedPhotosSize = (photosSize - screenshotsSize).coerceAtLeast(0L)

        // 6. Calculate System / Other partition
        val knownUserMediaBytes = adjustedPhotosSize + videosSize + screenshotsSize + appsSize + downloadsSize
        val systemAndOtherBytes = (calculatedUsedBytes - knownUserMediaBytes).coerceAtLeast(0L)

        // 7. Return Model
        return StorageStats(
            totalBytes = commercialTotalBytes,
            usedBytes = calculatedUsedBytes,
            freeBytes = actualFreeBytes,
            photosSizeBytes = adjustedPhotosSize,
            videosSizeBytes = videosSize,
            screenshotsSizeBytes = screenshotsSize,
            appsSizeBytes = appsSize,
            downloadsSizeBytes = downloadsSize,
            systemAndOtherSizeBytes = systemAndOtherBytes
        )
    }

    /**
     * Fallback path used when the [StorageStatsManager] APIs are unavailable
     * or throw. Uses [StatFs] against the legacy external storage root, which
     * requires no special permission and is available back to API 1 — less
     * precise (doesn't reflect StorageStatsManager's quota-aware accounting)
     * but always produces *a* usable number rather than crashing the caller.
     */
    private fun computeFallbackStats(cause: Throwable? = null): StorageStats {
        val externalDir = Environment.getExternalStorageDirectory()
        if (externalDir == null || !externalDir.exists()) {
            throw StorageUnavailableException(
                "Fallback external storage directory is unavailable",
                cause
            )
        }

        val statFs = try {
            StatFs(externalDir.path)
        } catch (e: IllegalArgumentException) {
            throw StorageUnavailableException("StatFs failed for ${externalDir.path}", e)
        }

        val rawTotalBytes = statFs.totalBytes
        val rawFreeBytes = statFs.availableBytes

        val commercialTotalBytes = StorageStats.roundToCommercialTotalBytes(rawTotalBytes)
        val actualFreeBytes = rawFreeBytes
        val calculatedUsedBytes = (commercialTotalBytes - actualFreeBytes).coerceAtLeast(0L)

        // Best-effort category breakdown even in fallback mode; if MediaStore
        // itself is the thing failing, degrade to zeros rather than throwing.
        val photos = sumMediaSize(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val videos = sumMediaSize(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        val screenshots = sumScreenshotSize()
        val downloads = sumDownloadsSize()

        val adjustedPhotos = (photos - screenshots).coerceAtLeast(0L)
        val knownUserMediaBytes = adjustedPhotos + videos + screenshots + downloads
        val systemAndOtherBytes = (calculatedUsedBytes - knownUserMediaBytes).coerceAtLeast(0L)

        return StorageStats(
            totalBytes = commercialTotalBytes,
            usedBytes = calculatedUsedBytes,
            freeBytes = actualFreeBytes,
            photosSizeBytes = adjustedPhotos,
            videosSizeBytes = videos,
            screenshotsSizeBytes = screenshots,
            downloadsSizeBytes = downloads,
            systemAndOtherSizeBytes = systemAndOtherBytes
        )
    }

    /**
     * Sums the `SIZE` column across every row visible at [collectionUri].
     *
     * A single narrow-projection cursor pass is used instead of per-row
     * detail queries — for large libraries (tens of thousands of items) this
     * keeps memory flat and avoids the cost of materializing full [MediaItem]
     * objects just to add up sizes.
     */
    private fun sumMediaSize(
        collectionUri: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): Long {
        val projection = arrayOf(MediaStore.MediaColumns.SIZE)
        var total = 0L

        appContext.contentResolver.query(
            collectionUri,
            projection,
            selection,
            selectionArgs,
            /* sortOrder = */ null
        )?.use { cursor ->
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            while (cursor.moveToNext()) {
                total += cursor.getLong(sizeColumn)
            }
        }

        return total
    }

    /**
     * Sums the size of images that live under a "Screenshots" bucket/path.
     * Matches both the legacy DCIM/Screenshots convention and the
     * Pictures/Screenshots convention used by RELATIVE_PATH on API 29+.
     */
    private fun sumScreenshotSize(): Long {
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR " +
                "${MediaStore.MediaColumns.BUCKET_DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf("%Screenshots%", "Screenshots")

        return sumMediaSize(
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            selection = selection,
            selectionArgs = selectionArgs
        )
    }

    /**
     * Sums the size of files in the Downloads directory using MediaStore.
     */
    private fun sumDownloadsSize(): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sumMediaSize(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
        } else {
            // Best effort for older APIs: query MediaStore.Files with a relative path filter
            val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("Download%")
            sumMediaSize(MediaStore.Files.getContentUri("external"), selection, selectionArgs)
        }
    }

    /**
     * Sums the total storage used by all applications for the current user.
     * Requires PACKAGE_USAGE_STATS permission for accurate results.
     */
    private fun sumAppsSize(volumeUuid: UUID): Long {
        return try {
            val stats = storageStatsManager?.queryStatsForUser(volumeUuid, Process.myUserHandle())
            // Sum app code, data, and cache to represent "Apps" category
            (stats?.appBytes ?: 0L) + (stats?.dataBytes ?: 0L) + (stats?.cacheBytes ?: 0L)
        } catch (e: Exception) {
            DataLogger.e("Failed to query app stats", e)
            0L
        }
    }
}
