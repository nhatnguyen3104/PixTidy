package com.androidtech.data.local.database

import androidx.room.*
import com.androidtech.data.model.MediaAnalysisCacheEntity
import com.androidtech.data.model.ScanSummaryEntity
import com.androidtech.data.model.SmartInsightEntity

@Database(
    entities = [
        MediaAnalysisCacheEntity::class,
        ScanSummaryEntity::class,
        SmartInsightEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(ScanResultConverters::class)
abstract class LocalDatabase: RoomDatabase() {

    companion object {
        const val DATABASE_NAME = "General_db"
    }

    abstract fun mediaCacheDao(): MediaCacheDao
    abstract fun scanResultsDao(): ScanResultsDao
}
