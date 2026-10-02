package com.androidtech.generalapps.di

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.ads.monetization.AdMobManager
import com.ads.monetization.AdManager
import com.androidtech.data.local.data_store.SettingsDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context = application

    @Provides
    @Singleton
    fun provideContentResolver(context: Context): ContentResolver = context.contentResolver

    @Singleton
    @Provides
    fun provideAdManager(context: Context): AdManager = AdMobManager(context)

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    /**
     * Provided via `PreferenceDataStoreFactory.create` (not the `Context.preferencesDataStore`
     * delegate) so the instance is created and owned by Hilt's singleton graph rather than an
     * ambient top-level property — the officially recommended pattern for DI'd DataStore, and it
     * keeps this class trivially fakeable in tests (swap the binding for an in-memory DataStore).
     */
    @Provides
    @Singleton
    fun provideSettingsDataStore(context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
            produceFile = { context.preferencesDataStoreFile(SettingsDataStore.PREFERENCES_NAME) }
        )
}