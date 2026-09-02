package com.androidtech.data.repository

import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.androidtech.data.source.SystemStorageDataSource
import com.androidtech.data.mapper.inferMediaType
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.MediaType
import com.androidtech.domain.model.media.StorageStats
import com.androidtech.domain.repository.MediaRepository
import com.androidtech.domain.repository.StorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * Thrown when a delete operation cannot proceed without the user confirming a system
 * dialog. Callers (typically a ViewModel) should catch this via [Result.exceptionOrNull]
 * and launch [pendingIntent] with `startIntentSenderForResult`, then retry the deletion
 * once the user grants consent.
 */
class DeleteConsentRequiredException(
    val pendingIntent: PendingIntent,
    val pendingUris: List<Uri>
) : Exception("User consent is required to delete ${pendingUris.size} item(s) on this Android version")

/**
 * [StorageRepository] implementation combining [SystemStorageDataSource] (raw capacity)
 * with [MediaRepository] (per-category byte totals) and performing deletions through
 * `MediaStore`, respecting each API level's scoped-storage deletion contract:
 *
 * - API 30+ (R): batched via [MediaStore.createDeleteRequest], which always requires
 *   user consent through a system dialog — surfaced here as [DeleteConsentRequiredException].
 * - API 29 (Q): per-item [android.content.ContentResolver.delete], recovering from
 *   [RecoverableSecurityException] by surfacing its embedded consent intent.
 * - API < 29: direct per-item delete, since scoped storage does not apply.
 */
class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val systemStorageDataSource: SystemStorageDataSource,
    private val mediaRepository: MediaRepository
) : StorageRepository {

    override fun getStorageStats(): Flow<StorageStats> = flow {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            emit(buildStorageStats())
            delay(STATS_REFRESH_INTERVAL_MS.milliseconds)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun buildStorageStats(): StorageStats {
        val (total, free) = systemStorageDataSource.getTotalAndFreeBytes()
        val used = (total - free).coerceAtLeast(0L)

        var photoBytes = 0L
        var videoBytes = 0L
        var screenshotBytes = 0L
        for (item in mediaRepository.queryAllMedia()) {
            when (item.inferMediaType()) {
                MediaType.SCREENSHOT -> screenshotBytes += item.sizeBytes
                MediaType.IMAGE -> photoBytes += item.sizeBytes
                MediaType.VIDEO -> videoBytes += item.sizeBytes
                MediaType.UNKNOWN -> Unit
            }
        }

        return StorageStats(
            totalBytes = total,
            usedBytes = used,
            freeBytes = free,
            photosSizeBytes = photoBytes,
            videosSizeBytes = videoBytes,
            screenshotsSizeBytes = screenshotBytes
        )
    }

    override suspend fun deleteMediaItems(items: List<MediaItem>): Result<Int> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext Result.success(0)

        val resolver = context.contentResolver
        val uris = items.map { it.uriString.toUri() }

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    // API 30+ always requires an explicit user consent dialog for batch deletes,
                    // even for files this app created itself. We cannot show that dialog from a
                    // suspend function, so we surface the PendingIntent for the UI layer to launch.
                    val pendingIntent = MediaStore.createDeleteRequest(resolver, uris)
                    Result.failure(DeleteConsentRequiredException(pendingIntent, uris))
                }

                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                    var deletedCount = 0
                    var firstConsentException: RecoverableSecurityException? = null
                    for (uri in uris) {
                        try {
                            if (resolver.delete(uri, null, null) > 0) deletedCount++
                        } catch (e: RecoverableSecurityException) {
                            // File owned by another app; remember the first consent request so
                            // the caller can prompt and retry, but keep deleting what we can.
                            if (firstConsentException == null) firstConsentException = e
                        }
                    }
                    if (deletedCount == 0 && firstConsentException != null) {
                        Result.failure(
                            DeleteConsentRequiredException(
                                firstConsentException.userAction.actionIntent,
                                uris
                            )
                        )
                    } else {
                        Result.success(deletedCount)
                    }
                }

                else -> {
                    // Pre-scoped-storage: direct deletion is permitted for app-visible media.
                    var deletedCount = 0
                    for (uri in uris) {
                        try {
                            if (resolver.delete(uri, null, null) > 0) deletedCount++
                        } catch (e: SecurityException) {
                            // Skip files we don't have permission to remove.
                        }
                    }
                    Result.success(deletedCount)
                }
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val STATS_REFRESH_INTERVAL_MS = 5_000L
    }
}