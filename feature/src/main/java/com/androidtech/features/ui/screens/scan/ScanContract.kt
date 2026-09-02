package com.androidtech.features.ui.screens.scan

import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.ScanProgress

data class ScanState(
    val scanProgress: ScanProgress = ScanProgress.Idle,
) : UIState

sealed class ScanEvent : UIEvent {
    data object StartScan : ScanEvent()
    data object CancelScan : ScanEvent()
    data object NavigateBack : ScanEvent()
}

sealed class ScanEffect : UIEffect {
    data object NavigateBack : ScanEffect()
    // You could add NavigateToResults(groups) here if you want to move to another screen
}