package com.androidtech.generalapps.di

import android.content.Context
import androidx.room.Room
import com.androidtech.data.local.database.LocalDatabase
import com.androidtech.data.local.database.MediaCacheDao
import com.androidtech.data.local.database.ScanResultsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.jvm.java

@Module
@InstallIn(SingletonComponent::class)
object LocalDatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase( @ApplicationContext context: Context): LocalDatabase =
        Room.databaseBuilder(
            context,
            LocalDatabase::class.java,
            LocalDatabase.DATABASE_NAME
        )
            // The cache is fully derivable by re-scanning, so a destructive fallback on schema
            // change is an acceptable trade-off versus hand-writing migrations for a pure cache table.
            .fallbackToDestructiveMigration()
            .build()

    @Singleton
    @Provides
    fun provideMediaCacheDao(localDatabase: LocalDatabase): MediaCacheDao = localDatabase.mediaCacheDao()

    @Singleton
    @Provides
    fun provideScanResultsDao(localDatabase: LocalDatabase): ScanResultsDao = localDatabase.scanResultsDao()
}
