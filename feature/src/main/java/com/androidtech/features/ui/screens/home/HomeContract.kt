package com.androidtech.features.ui.screens.home

import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIState

data class HomeState(
    val isLoading: Boolean = false,
): UIState

sealed class HomeEvent : UIEvent {

}

sealed class HomeEffect : UIEffect {
}