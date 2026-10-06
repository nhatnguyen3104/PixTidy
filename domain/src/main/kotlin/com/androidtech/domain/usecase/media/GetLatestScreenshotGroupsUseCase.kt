package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.UIStateUseCase
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.ScreenshotGroup
import com.androidtech.domain.model.media.SmartInsightType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Fetches the latest "Screenshots" insight and reconstructs the [ScreenshotGroup] structure
 * from the flattened list of items, using the [com.androidtech.domain.model.media.MediaItem.groupId] tag.
 */
class GetLatestScreenshotGroupsUseCase @Inject constructor(
    private val getLatestSmartInsightsUseCase: GetLatestSmartInsightsUseCase
) : UIStateUseCase<Unit, List<ScreenshotGroup>>() {

    override fun run(param: Unit): Flow<UiState<List<ScreenshotGroup>>> {
        return getLatestSmartInsightsUseCase(null).map { uiState ->
            when (uiState) {
                is UiState.Idle -> UiState.Idle
                is UiState.Loading -> UiState.Loading(uiState.progress)
                is UiState.Error -> UiState.Error(uiState.failure)
                is UiState.Success -> {
                    val screenshotInsights = uiState.data.filter {
                        it.type == SmartInsightType.SCREENSHOTS
                    }

                    val groups = screenshotInsights
                        .flatMap { it.items }
                        .groupBy { it.groupId }
                        .mapNotNull { (groupId, items) ->
                            if (groupId == null) return@mapNotNull null
                            val timeLabel = when (groupId) {
                                "screenshot_over_1_year" -> "Older than 1 year"
                                "screenshot_60_to_365_days" -> "60 to 365 days old"
                                "screenshot_30_to_60_days" -> "30 to 60 days old"
                                else -> "Older than 30 days"
                            }

                            ScreenshotGroup(
                                groupId = groupId,
                                screenshotItems = items,
                                timeLabel = timeLabel,
                                potentialSavingsBytes = items.sumOf { it.size }
                            )
                        }
                        .sortedByDescending { group ->
                            when (group.groupId) {
                                "screenshot_over_1_year" -> 3
                                "screenshot_60_to_365_days" -> 2
                                else -> 1
                            }
                        }
                    UiState.Success(groups)
                }
            }
        }
    }
}
