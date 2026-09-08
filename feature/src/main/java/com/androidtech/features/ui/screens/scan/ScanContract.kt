package com.androidtech.features.ui.screens.scan

import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProcess

data class ScanState(
    val scanLayer: ScanLayer = ScanLayer.FAST_SCAN,
    val scanProcess: ScanProcess = ScanProcess.Idle
) : UIState

sealed class ScanEvent : UIEvent {
    data object StartScanClick : ScanEvent()
    data object CancelScanClick : ScanEvent()
    data object NavigateBackClick : ScanEvent()
}

sealed class ScanEffect : UIEffect {
    data object NavigateBack : ScanEffect()
    // You could add NavigateToResults(groups) here if you want to move to another screen
}
