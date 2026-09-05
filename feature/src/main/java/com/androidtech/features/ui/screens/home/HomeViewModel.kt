package com.androidtech.features.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.usecase.media.RunFastScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runFastScanUseCase: RunFastScanUseCase
): BaseViewModel<HomeEvent, HomeState, HomeEffect>(savedStateHandle) {
    override fun createInitializeState(): HomeState = HomeState()

    private var scanJob: Job? = null

    init {
        fastScan()
    }

    private fun fastScan() {
        scanJob?.cancel()
        scanJob = runFastScanUseCase()
            .onEach { summary ->
                setState { copy(storageStats = summary) }
            }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: HomeEvent) {
        when(event) {
            is HomeEvent.CategoryClick -> {

            }
            is HomeEvent.SmartScanClicked -> {
                setEffect { HomeEffect.NavigateToSmartScan }
            }
            is HomeEvent.DeepScanClicked -> {
                setEffect { HomeEffect.NavigateToDeepScan }
            }
            is HomeEvent.SettingsClicked -> {
                setEffect { HomeEffect.NavigateToSettings }
            }
            HomeEvent.StartScan -> {
                fastScan()
            }
        }
    }
}
