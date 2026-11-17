package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.UIStateUseCase
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.LargeVideoGroup
import com.androidtech.domain.model.media.SmartInsightType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetLatestLargeVideoGroupsUseCase @Inject constructor(
    private val getLatestSmartInsightsUseCase: GetLatestSmartInsightsUseCase
) : UIStateUseCase<Unit, List<LargeVideoGroup>>() {

    override fun run(param: Unit): Flow<UiState<List<LargeVideoGroup>>> {
        return getLatestSmartInsightsUseCase(null).map { uiState ->
            when (uiState) {
                is UiState.Idle -> UiState.Idle
                is UiState.Loading -> UiState.Loading(uiState.progress)
                is UiState.Error -> UiState.Error(uiState.failure)
                is UiState.Success -> {
                    val insights = uiState.data.filter {
                        it.type == SmartInsightType.LARGE_VIDEOS
                    }

                    val groups = insights
                        .flatMap { it.items }
                        .groupBy { it.groupId }
                        .mapNotNull { (groupId, items) ->
                            if (groupId == null) return@mapNotNull null
                            val label = when (groupId) {
                                "large_video_screen_recordings" -> "Screen Recordings"
                                "large_video_old_and_long" -> "Old & Long Videos"
                                "large_video_massive" -> "large capacity (>100MB)"
                                else -> "Large Videos"
                            }

                            LargeVideoGroup(
                                groupId = groupId,
                                videoItems = items,
                                categoryLabel = label,
                                potentialSavingsBytes = items.sumOf { it.size }
                            )
                        }
                        .sortedByDescending { group ->
                            when (group.groupId) {
                                "large_video_massive" -> 3
                                "large_video_old_and_long" -> 2
                                else -> 1
                            }
                        }
                    UiState.Success(groups)
                }
            }
        }
    }
}
