package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.UIStateUseCase
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.FastScanSummary
import com.androidtech.domain.repository.ScanResultsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetLatestScanResultsUseCase @Inject constructor(
    private val scanResultsRepository: ScanResultsRepository
): UIStateUseCase<Unit, FastScanSummary?>() {

    override fun run(param: Unit): Flow<UiState<FastScanSummary?>> {
        return scanResultsRepository.getLatestResults().map { summary ->
            if (summary != null) UiState.Success(summary)
            else UiState.Success(FastScanSummary.EMPTY)
        }
    }
}
