package com.androidtech.domain.usecase.storage

import com.androidtech.domain.extensions.AppFailure
import com.androidtech.domain.extensions.Either
import com.androidtech.domain.extensions.UseCase
import com.androidtech.domain.model.media.StorageStats
import com.androidtech.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetStorageStatsUseCase @Inject constructor(
    private val storageRepository: StorageRepository
): UseCase<Unit, StorageStats>() {
    override fun run(param: Unit): Flow<Either<AppFailure, StorageStats>> = flow {
        storageRepository.getStorageStatsSnapshot()
    }
}