package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.UIStateUseCase
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.SmartInsight
import com.androidtech.domain.repository.ScanResultsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetLatestSmartInsightsUseCase @Inject constructor(
    private val scanResultsRepository: ScanResultsRepository
) : UIStateUseCase<Unit, List<SmartInsight>>() {
    override fun run(param: Unit): Flow<UiState<List<SmartInsight>>> {
        return scanResultsRepository.getLatestInsights().map { insights ->
            UiState.Success(insights)
        }
    }
}
