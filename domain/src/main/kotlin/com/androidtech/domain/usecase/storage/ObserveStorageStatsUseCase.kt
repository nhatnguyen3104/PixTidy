package com.androidtech.domain.usecase.storage

import com.androidtech.domain.extensions.AppFailure
import com.androidtech.domain.extensions.UIStateUseCase
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.StorageStats
import com.androidtech.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class ObserveStorageStatsUseCase @Inject constructor(
    private val storageRepository: StorageRepository
): UIStateUseCase<Unit, StorageStats>() {
    override fun run(param: Unit): Flow<UiState<StorageStats>> {
        return storageRepository.observeStorageStats().map { UiState.Success(it) as UiState<StorageStats> }
            .onStart { emit(UiState.Loading()) }
            .catch { emit(UiState.Error(AppFailure.Unknown(it))) }
    }
}