package com.androidtech.generalapps.di

import android.content.Context
import androidx.room.Room
import com.androidtech.data.local.database.LocalDatabase
import com.androidtech.data.local.database.PdfDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.jvm.java

@Module
@InstallIn(SingletonComponent::class)
object LocalDatabaseModule {

    @Singleton
    @Provides
    fun provideAppDatabase(context: Context): LocalDatabase =
        Room.databaseBuilder(
            context,
            LocalDatabase::class.java,
            LocalDatabase.DATABASE_NAME
        ).build()

    @Singleton
    @Provides
    fun provicePdfDao(localDatabase: LocalDatabase): PdfDao = localDatabase.getPdfDao()
}