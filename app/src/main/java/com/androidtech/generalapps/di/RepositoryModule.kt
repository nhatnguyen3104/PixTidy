package com.androidtech.generalapps.di

import com.androidtech.data.repository.RemoteConfigRepositoryImp
import com.androidtech.domain.repository.RemoteConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    fun bindRemoteConfigRepository(impl: RemoteConfigRepositoryImp): RemoteConfigRepository
}