package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.UIStateUseCase
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.DuplicateGroup
import com.androidtech.domain.model.media.SmartInsightType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Fetches the latest "Exact Duplicates" insight and reconstructs the [DuplicateGroup] structure
 * from the flattened list of items, using the [com.androidtech.domain.model.media.MediaItem.groupId]
 * and [com.androidtech.domain.model.media.MediaItem.isBestKeep] tags.
 */
class GetLatestDuplicateGroupsUseCase @Inject constructor(
    private val getLatestSmartInsightsUseCase: GetLatestSmartInsightsUseCase
) : UIStateUseCase<Unit, List<DuplicateGroup>>() {

    override fun run(param: Unit): Flow<UiState<List<DuplicateGroup>>> {
        return getLatestSmartInsightsUseCase(null).map { uiState ->
            when (uiState) {
                is UiState.Idle -> UiState.Idle
                is UiState.Loading -> UiState.Loading(uiState.progress)
                is UiState.Error -> UiState.Error(uiState.failure)
                is UiState.Success -> {
                    val duplicateInsights = uiState.data.filter { 
                        it.type == SmartInsightType.EXACT_DUPLICATES || 
                        it.type == SmartInsightType.DUPLICATE_VIDEOS 
                    }
                    
                    val groups = duplicateInsights
                        .flatMap { it.items }
                        .groupBy { it.groupId }
                        .mapNotNull { (groupId, items) ->
                            if (groupId == null) return@mapNotNull null
                            val original = items.find { it.isBestKeep } ?: items.first()
                            val duplicates = items.filterNot { it.id == original.id }
                            if (duplicates.isEmpty()) return@mapNotNull null

                            DuplicateGroup(
                                groupId = groupId,
                                originalItem = original,
                                duplicateItems = duplicates,
                                totalSizeDifferenceBytes = duplicates.sumOf { it.size },
                                hash = "" // Hash not strictly needed for UI display if we have ID/Group
                            )
                        }
                    UiState.Success(groups)
                }
            }
        }
    }
}
