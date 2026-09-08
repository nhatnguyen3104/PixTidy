package com.androidtech.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.androidtech.data.model.ScanSummaryEntity
import com.androidtech.data.model.SmartInsightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultsDao {

    @Query("SELECT * FROM scan_summary WHERE id = 1")
    fun getLatestSummary(): Flow<ScanSummaryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: ScanSummaryEntity)

    @Query("SELECT * FROM smart_insights")
    fun getAllInsights(): Flow<List<SmartInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<SmartInsightEntity>)

    @Query("DELETE FROM smart_insights")
    suspend fun deleteAllInsights()

    @Transaction
    suspend fun updateScanResults(summary: ScanSummaryEntity, insights: List<SmartInsightEntity>) {
        insertSummary(summary)
        deleteAllInsights()
        insertInsights(insights)
    }

    @Transaction
    suspend fun clearAllResults() {
        deleteAllInsights()
        // We could delete summary or reset it. Let's delete for now if we want a fresh start.
        // But usually we just want to clear insights if things were cleaned.
    }
}
