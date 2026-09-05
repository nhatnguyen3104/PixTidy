package com.androidtech.features.ui.screens.home

import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.FastScanSummary

data class HomeState(
    val isLoading: Boolean = false,
    val storageStats: FastScanSummary = FastScanSummary.EMPTY,
): UIState

sealed class HomeEvent : UIEvent {
    data class CategoryClick (val category: String) : HomeEvent()
    object DeepScanClicked : HomeEvent()
    object SmartScanClicked : HomeEvent()
    object SettingsClicked : HomeEvent()
    object StartScan : HomeEvent()
}

sealed class HomeEffect : UIEffect {
    object NavigateToDeepScan : HomeEffect()
    object NavigateToSmartScan : HomeEffect()
    object NavigateToSettings : HomeEffect()
}