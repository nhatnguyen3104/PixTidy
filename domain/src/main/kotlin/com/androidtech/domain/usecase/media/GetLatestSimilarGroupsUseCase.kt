package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.UIStateUseCase
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.SimilarGroup
import com.androidtech.domain.model.media.SmartInsightType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Fetches the latest "Similar Photos" insight and reconstructs the [SimilarGroup] structure
 * from the flattened list of items, using the [com.androidtech.domain.model.media.MediaItem.groupId]
 * and [com.androidtech.domain.model.media.MediaItem.isBestKeep] tags.
 */
class GetLatestSimilarGroupsUseCase @Inject constructor(
    private val getLatestSmartInsightsUseCase: GetLatestSmartInsightsUseCase
) : UIStateUseCase<Unit, List<SimilarGroup>>() {

    override fun run(param: Unit): Flow<UiState<List<SimilarGroup>>> {
        return getLatestSmartInsightsUseCase(null).map { uiState ->
            when (uiState) {
                is UiState.Idle -> UiState.Idle
                is UiState.Loading -> UiState.Loading(uiState.progress)
                is UiState.Error -> UiState.Error(uiState.failure)
                is UiState.Success -> {
                    val similarInsights = uiState.data.filter {
                        it.type == SmartInsightType.SIMILAR_PHOTOS
                    }

                    val groups = similarInsights
                        .flatMap { it.items }
                        .groupBy { it.groupId }
                        .mapNotNull { (groupId, items) ->
                            if (groupId == null) return@mapNotNull null
                            val bestCandidate = items.find { it.isBestKeep } ?: items.first()
                            
                            SimilarGroup(
                                groupId = groupId,
                                items = items,
                                bestCandidateId = bestCandidate.id,
                                similarityPercentage = items.firstOrNull()?.similarityPercentage ?: 85,
                                potentialSavingsBytes = items.filter { it.id != bestCandidate.id }.sumOf { it.size }
                            )
                        }
                    UiState.Success(groups)
                }
            }
        }
    }
}
