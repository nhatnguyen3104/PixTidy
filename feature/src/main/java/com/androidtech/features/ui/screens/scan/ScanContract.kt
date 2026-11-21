package com.androidtech.features.ui.screens.scan

import com.ads.monetization.adholder.BannerAdHolder
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProcess
import com.androidtech.domain.model.media.SmartInsightType
import com.androidtech.features.ui.screens.home.HomeEffect

data class ScanState(
    val scanLayer: ScanLayer = ScanLayer.NONE,
    val scanProcess: ScanProcess = ScanProcess.Idle,
    val bannerAdHolder: BannerAdHolder? = null
) : UIState

sealed class ScanEvent : UIEvent {
    data object StartScan : ScanEvent()
    data object CancelScanClick : ScanEvent()
    data class SmartInsightTypeClick (val smartInsightType: SmartInsightType) : ScanEvent()
    data object NavigateBackClick : ScanEvent()
}

sealed class ScanEffect : UIEffect {
    data object NavigateBack : ScanEffect()
    object NavigateToDuplicate : ScanEffect()
    object NavigateToSimilar : ScanEffect()
    object NavigareToScreenshot : ScanEffect()
    object NavigateToLargeVideo : ScanEffect()
    object NavigateToBlurry : ScanEffect()
    object NavigateToOcrDocument : ScanEffect()
}
