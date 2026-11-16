package com.androidtech.features.ui.screens.largevideo

import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState

data class LargeVideoState (
    val isLoading: Boolean? = false
): UIState

sealed class LargeVideoEvent : UIEvent {

}

sealed class LargeVideoEffect : UIEffect {

}