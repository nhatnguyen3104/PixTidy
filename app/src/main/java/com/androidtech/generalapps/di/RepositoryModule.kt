package com.androidtech.generalapps.di

import com.androidtech.data.analyzer.ImageAnalyzerEngineImpl
import com.androidtech.data.engine.DeepAnalysisEngineImpl
import com.androidtech.data.engine.RecommendationEngineImpl
import com.androidtech.data.repository.*
import com.androidtech.domain.engine.DeepAnalysisEngine
import com.androidtech.domain.engine.ImageAnalyzerEngine
import com.androidtech.domain.engine.RecommendationEngine
import com.androidtech.domain.repository.*
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
    fun bindMediaAnalysisCacheRepository(impl: MediaAnalysisCacheRepositoryImpl): MediaAnalysisCacheRepository

    @Binds
    fun bindScanResultsRepository(impl: ScanResultsRepositoryImpl): ScanResultsRepository

    @Binds
    fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    fun bindImageAnalyzerEngine(impl: ImageAnalyzerEngineImpl): ImageAnalyzerEngine

    @Binds
    fun bindDeepAnalysisEngine(impl: DeepAnalysisEngineImpl): DeepAnalysisEngine

    @Binds
    fun bindRecommendationEngine(impl: RecommendationEngineImpl): RecommendationEngine

    @Binds
    fun bindLocaleController(impl: AndroidLocaleController): LocaleController

}
