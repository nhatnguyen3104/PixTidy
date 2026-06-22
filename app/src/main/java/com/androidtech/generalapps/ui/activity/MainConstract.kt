package com.androidtech.generalapps.ui.activity

import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState

sealed class MainEvent(): UIEvent {
    data object LoadRemoteConfig: MainEvent()
}

data class MainState(
    val testData: String = ""
): UIState

sealed class MainEffect: UIEffect {
    data object TestEffect: MainEffect()
}