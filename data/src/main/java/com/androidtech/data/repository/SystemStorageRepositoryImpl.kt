package com.androidtech.data.repository

import com.androidtech.data.datasource.SystemStorageDataSource
import com.androidtech.domain.model.media.StorageSummary
import com.androidtech.domain.repository.SystemStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SystemStorageRepositoryImpl @Inject constructor(
    private val systemStorageDataSource: SystemStorageDataSource
) : SystemStorageRepository {

    override suspend fun fetchDeviceStorage(): StorageSummary = withContext(Dispatchers.IO) {
        systemStorageDataSource.readDeviceStorage()
    }
}