package com.androidtech.generalapps.di

import com.androidtech.data.analyzer.ImageAnalyzerEngineImpl
import com.androidtech.data.engine.DeepAnalysisEngineImpl
import com.androidtech.data.engine.RecommendationEngineImpl
import com.androidtech.data.repository.MediaStoreRepositoryImpl
import com.androidtech.data.repository.RemoteConfigRepositoryImp
import com.androidtech.data.repository.SystemStorageRepositoryImpl
import com.androidtech.domain.engine.DeepAnalysisEngine
import com.androidtech.domain.engine.ImageAnalyzerEngine
import com.androidtech.domain.engine.RecommendationEngine
import com.androidtech.domain.repository.MediaStoreRepository
import com.androidtech.domain.repository.RemoteConfigRepository
import com.androidtech.domain.repository.SystemStorageRepository
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
    fun bindMediaStoreRepository(impl: MediaStoreRepositoryImpl): MediaStoreRepository

    @Binds
    fun bindStorageRepository(impl: SystemStorageRepositoryImpl): SystemStorageRepository

    @Binds
    abstract fun bindImageAnalyzerEngine(impl: ImageAnalyzerEngineImpl): ImageAnalyzerEngine

    @Binds
    abstract fun bindDeepAnalysisEngine(impl: DeepAnalysisEngineImpl): DeepAnalysisEngine

    @Binds
    abstract fun bindRecommendationEngine(impl: RecommendationEngineImpl): RecommendationEngine

}
