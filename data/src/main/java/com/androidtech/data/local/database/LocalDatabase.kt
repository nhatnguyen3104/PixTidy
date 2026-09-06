package com.androidtech.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.androidtech.data.model.MediaAnalysisCacheEntity

@Database(
    entities = [MediaAnalysisCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocalDatabase: RoomDatabase() {

    companion object {
        const val DATABASE_NAME = "General_db"
    }

    abstract fun mediaCacheDao(): MediaCacheDao
}