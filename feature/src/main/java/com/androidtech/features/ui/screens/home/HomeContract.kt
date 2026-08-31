package com.androidtech.features.ui.screens.home

import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.StorageStats

data class HomeState(
    val isLoading: Boolean = false,
    val storageStats: StorageStats = StorageStats(0L, 1L, 0L),
): UIState

sealed class HomeEvent : UIEvent {
    data class CategoryClick (val category: String) : HomeEvent()
    object ScanClicked : HomeEvent()
    object SettingsClicked : HomeEvent()
}

sealed class HomeEffect : UIEffect {
    object NavigateToScan : HomeEffect()

}