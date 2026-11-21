package com.androidtech.data.repository

import com.androidtech.data.local.InMemoryAnalysisCache
import com.androidtech.data.model.MediaAnalysisCacheEntity
import com.androidtech.data.local.database.MediaCacheDao
import com.androidtech.domain.model.media.CachedMediaAnalysis
import com.androidtech.domain.model.media.FrameSample
import com.androidtech.domain.repository.MediaAnalysisCacheRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Two-tier cache: [InMemoryAnalysisCache] (fast, session-scoped) in front of [MediaCacheDao]
 * (durable, survives app restarts). Reads check memory first; a Room hit backfills memory so the
 * next lookup for that item is free for the rest of the session. Writes update both tiers so they
 * never disagree within a session.
 */
class MediaAnalysisCacheRepositoryImpl @Inject constructor(
    private val mediaCacheDao: MediaCacheDao,
    private val inMemoryAnalysisCache: InMemoryAnalysisCache,
    private val moshi: Moshi
) : MediaAnalysisCacheRepository {

    private val frameSampleListAdapter = moshi.adapter<List<FrameSample>>(
        Types.newParameterizedType(List::class.java, FrameSample::class.java)
    )

    override suspend fun getValidCache(mediaId: Long, dateModified: Long): CachedMediaAnalysis? {
        inMemoryAnalysisCache.get(mediaId)?.let { memoryHit ->
            // The in-memory entry doesn't carry dateModified (it's already known-valid at the
            // moment it was written this session), so it's always trusted once present — a stale
            // file would have produced a different dateModified and thus a different Room lookup
            // path the *first* time it was seen this session, which is the only time memory is
            // populated for that id.
            return memoryHit
        }

        return withContext(Dispatchers.IO) {
            val entity = mediaCacheDao.getCacheForMedia(mediaId) ?: return@withContext null
            if (entity.dateModified != dateModified) return@withContext null // stale: file changed since caching

            entity.toDomain().also { inMemoryAnalysisCache.put(it) }
        }
    }

    override suspend fun saveAnalysis(
        mediaId: Long,
        dateModified: Long,
        phash: String?,
        blurScore: Int,
        isScreenshot: Boolean
    ) {
        val existing = withContext(Dispatchers.IO) {
            mediaCacheDao.getCacheForMedia(mediaId)
        }
        val entity = MediaAnalysisCacheEntity(
            mediaId = mediaId,
            dateModified = dateModified,
            pHash = phash ?: existing?.pHash,
            blurScore = blurScore,
            isScreenshot = isScreenshot,
            videoFramesJson = existing?.videoFramesJson,
            isDocument = existing?.isDocument,
            documentConfidence = existing?.documentConfidence,
            ocrPreview = existing?.ocrPreview,
            lastAnalyzed = System.currentTimeMillis()
        )

        withContext(Dispatchers.IO) {
            mediaCacheDao.insertOrUpdateCache(entity)
        }
        inMemoryAnalysisCache.put(entity.toDomain())
    }

    override suspend fun saveVideoFrames(
        mediaId: Long,
        dateModified: Long,
        videoFrames: List<FrameSample>
    ) {
        val existing = withContext(Dispatchers.IO) {
            mediaCacheDao.getCacheForMedia(mediaId)
        }
        val framesJson = runCatching { frameSampleListAdapter.toJson(videoFrames) }.getOrNull()
        val entity = MediaAnalysisCacheEntity(
            mediaId = mediaId,
            dateModified = dateModified,
            pHash = existing?.pHash,
            blurScore = existing?.blurScore ?: 50,
            isScreenshot = existing?.isScreenshot ?: false,
            videoFramesJson = framesJson,
            isDocument = existing?.isDocument,
            documentConfidence = existing?.documentConfidence,
            ocrPreview = existing?.ocrPreview,
            lastAnalyzed = System.currentTimeMillis()
        )
        withContext(Dispatchers.IO) {
            mediaCacheDao.insertOrUpdateCache(entity)
        }
        inMemoryAnalysisCache.put(entity.toDomain())
    }

    override suspend fun saveDocumentAnalysis(
        mediaId: Long,
        dateModified: Long,
        isDocument: Boolean,
        confidence: Float,
        preview: String?
    ) {
        val existing = withContext(Dispatchers.IO) {
            mediaCacheDao.getCacheForMedia(mediaId)
        }
        val entity = MediaAnalysisCacheEntity(
            mediaId = mediaId,
            dateModified = dateModified,
            pHash = existing?.pHash,
            blurScore = existing?.blurScore ?: 50,
            isScreenshot = existing?.isScreenshot ?: false,
            videoFramesJson = existing?.videoFramesJson,
            isDocument = isDocument,
            documentConfidence = confidence,
            ocrPreview = preview,
            lastAnalyzed = System.currentTimeMillis()
        )
        withContext(Dispatchers.IO) {
            mediaCacheDao.insertOrUpdateCache(entity)
        }
        inMemoryAnalysisCache.put(entity.toDomain())
    }

    override suspend fun pruneOrphaned(validMediaIds: List<Long>) {
        withContext(Dispatchers.IO) {
            mediaCacheDao.deleteOrphanedCaches(validMediaIds)
        }
        inMemoryAnalysisCache.retainOnly(validMediaIds.toSet())
    }

    private fun MediaAnalysisCacheEntity.toDomain(): CachedMediaAnalysis {
        val frames = try {
            videoFramesJson?.let { frameSampleListAdapter.fromJson(it) }
        } catch (e: Exception) {
            null
        }
        return CachedMediaAnalysis(
            mediaId = mediaId,
            phash = pHash,
            blurScore = blurScore,
            isScreenshot = isScreenshot,
            videoFrames = frames,
            isDocument = isDocument,
            documentConfidence = documentConfidence,
            ocrPreview = ocrPreview
        )
    }
}
