package com.androidtech.features.ui.screens.home

import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.ScanSummary
import com.androidtech.domain.model.media.SmartInsightType
import com.androidtech.domain.model.media.StorageSummary

data class HomeState(
    val isLoading: Boolean = false,
    val isPermissionGranted: Boolean = false,
    //val storageSummary: StorageSummary = StorageSummary.UNKNOWN,
    val scanSummary: ScanSummary = ScanSummary.EMPTY
): UIState

sealed class HomeEvent : UIEvent {
    data class SmartInsightTypeClick (val smartInsightType: SmartInsightType) : HomeEvent()
    object DeepScanClicked : HomeEvent()
    object SmartScanClicked : HomeEvent()
    object SettingsClicked : HomeEvent()
    object FastScan : HomeEvent()
    object RequestPermissionClicked : HomeEvent()
    data class PermissionStatusChanged(val isGranted: Boolean) : HomeEvent()
}

sealed class HomeEffect : UIEffect {
    object NavigateToDeepScan : HomeEffect()
    object NavigateToSmartScan : HomeEffect()
    object NavigateToSettings : HomeEffect()
    object NavigateToDuplicate : HomeEffect()
    object NavigateToSimilar : HomeEffect()
    object NavigateToScreenshot : HomeEffect()
    object RequestStoragePermissions : HomeEffect()
}
