package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.UIStateUseCase
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.SmartInsight
import com.androidtech.domain.model.media.SmartInsightType
import com.androidtech.domain.repository.ScanResultsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetLatestSmartInsightsUseCase @Inject constructor(
    private val scanResultsRepository: ScanResultsRepository
) : UIStateUseCase<SmartInsightType?, List<SmartInsight>>() {
    override fun run(param: SmartInsightType?): Flow<UiState<List<SmartInsight>>> {
        return scanResultsRepository.getLatestInsights().map { insights ->
            val filtered = if (param != null) {
                insights.filter { it.type == param }
            } else {
                insights
            }
            UiState.Success(filtered)
        }
    }
}
