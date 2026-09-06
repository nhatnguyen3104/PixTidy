package com.androidtech.data.repository

import com.androidtech.data.local.InMemoryAnalysisCache
import com.androidtech.data.model.MediaAnalysisCacheEntity
import com.androidtech.data.local.database.MediaCacheDao
import com.androidtech.domain.model.media.CachedMediaAnalysis
import com.androidtech.domain.repository.MediaAnalysisCacheRepository
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
    private val inMemoryAnalysisCache: InMemoryAnalysisCache
) : MediaAnalysisCacheRepository {

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
        val entity = MediaAnalysisCacheEntity(
            mediaId = mediaId,
            dateModified = dateModified,
            pHash = phash,
            blurScore = blurScore,
            isScreenshot = isScreenshot,
            lastAnalyzed = System.currentTimeMillis()
        )

        withContext(Dispatchers.IO) {
            mediaCacheDao.insertOrUpdateCache(entity)
        }
        inMemoryAnalysisCache.put(entity.toDomain())
    }

    override suspend fun pruneOrphaned(validMediaIds: List<Long>) {
        withContext(Dispatchers.IO) {
            // NOTE: a single `NOT IN (...)` query cannot safely be split into chunks — each chunk
            // would incorrectly delete rows belonging to ids in the *other* chunks, since "not in
            // this chunk" is true for them too. This is fine up to SQLite's bound-parameter limit
            // (999 on older SQLite, tens of thousands on the version bundled with modern Android);
            // a library that large should switch this to a staging temp table instead of a direct
            // NOT IN, but that's out of scope for the DAO surface as specified.
            mediaCacheDao.deleteOrphanedCaches(validMediaIds)
        }
        inMemoryAnalysisCache.retainOnly(validMediaIds.toSet())
    }

    private fun MediaAnalysisCacheEntity.toDomain() = CachedMediaAnalysis(
        mediaId = mediaId,
        phash = pHash,
        blurScore = blurScore,
        isScreenshot = isScreenshot
    )
}
