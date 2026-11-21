package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.UIStateUseCase
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.SmartInsightType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetLatestOcrDocumentUseCase @Inject constructor(
    private val getLatestSmartInsightsUseCase: GetLatestSmartInsightsUseCase
): UIStateUseCase<Unit, List<MediaItem>>() {
    override fun run(param: Unit): Flow<UiState<List<MediaItem>>> {
        return getLatestSmartInsightsUseCase(null).map { uiState ->
            when (uiState) {
                is UiState.Idle -> UiState.Idle
                is UiState.Loading -> UiState.Loading(uiState.progress)
                is UiState.Error -> UiState.Error(uiState.failure)
                is UiState.Success -> {
                    val documentInsights = uiState.data.filter { it.type == SmartInsightType.DEEP_OCR_DOCUMENTS }
                    val documentItems = documentInsights.flatMap { it.items }
                    UiState.Success(documentItems)
                }
            }
        }
    }
}