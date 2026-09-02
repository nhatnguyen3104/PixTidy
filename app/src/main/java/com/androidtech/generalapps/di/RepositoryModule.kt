package com.androidtech.generalapps.di

import com.androidtech.data.engine.StorageIntelligenceEngineImpl
import com.androidtech.data.repository.MediaRepositoryImpl
import com.androidtech.data.repository.RemoteConfigRepositoryImp
import com.androidtech.data.repository.StorageRepositoryImpl
import com.androidtech.domain.repository.MediaRepository
import com.androidtech.domain.repository.RemoteConfigRepository
import com.androidtech.domain.repository.StorageIntelligenceEngine
import com.androidtech.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    fun bindRemoteConfigRepository(impl: RemoteConfigRepositoryImp): RemoteConfigRepository

    @Binds
    fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    fun bindStorageIntelligenceEngine(impl: StorageIntelligenceEngineImpl): StorageIntelligenceEngine

}
