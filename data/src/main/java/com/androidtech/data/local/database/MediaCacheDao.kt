package com.androidtech.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.androidtech.data.model.MediaAnalysisCacheEntity

@Dao
interface MediaCacheDao {

    @Query("SELECT * FROM media_analysis_cache WHERE media_id = :mediaId LIMIT 1")
    suspend fun getCacheForMedia(mediaId: Long): MediaAnalysisCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCache(cache: MediaAnalysisCacheEntity)

    /**
     * Deletes every cache row whose media_id is not present in [validMediaIds] — i.e. media that
     * has been deleted, moved, or otherwise no longer exists. Safe up to SQLite's bound-parameter
     * limit; see [com.androidtech.data.repository.MediaAnalysisCacheRepositoryImpl.pruneOrphaned]
     * for the caveat on very large libraries.
     */
    @Query("DELETE FROM media_analysis_cache WHERE media_id NOT IN (:validMediaIds)")
    suspend fun deleteOrphanedCaches(validMediaIds: List<Long>)
}