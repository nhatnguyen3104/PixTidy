package com.androidtech.features.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.FastScanSummary
import com.androidtech.domain.usecase.media.GetLatestScanResultsUseCase
import com.androidtech.domain.usecase.media.RunFastScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runFastScanUseCase: RunFastScanUseCase,
    private val getLatestScanResultsUseCase: GetLatestScanResultsUseCase
): BaseViewModel<HomeEvent, HomeState, HomeEffect>(savedStateHandle) {
    override fun createInitializeState(): HomeState = HomeState()

    private var scanJob: Job? = null

    init {
        observeScanResults()
        fastScan()
    }

    private fun observeScanResults() {
        getLatestScanResultsUseCase(Unit)
            .filterIsInstance<UiState.Success<FastScanSummary?>>()
            .onEach { state ->
                state.data?.let { summary ->
                    setState { copy(storageStats = summary) }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun fastScan() {
        scanJob?.cancel()
        scanJob = combine(
            runFastScanUseCase(),
            getLatestScanResultsUseCase(Unit)
                .filterIsInstance<UiState.Success<FastScanSummary?>>()
                .map { it.data }
        ) { fresh, cached ->
            fresh.copy(
                duplicatesSizeBytes = cached?.duplicatesSizeBytes ?: 0L,
                screenshotCount = if ((cached?.screenshotCount ?: 0) > 0) cached!!.screenshotCount else fresh.screenshotCount,
                screenshotBytes = if ((cached?.screenshotBytes ?: 0L) > 0L) cached!!.screenshotBytes else fresh.screenshotBytes,
                largeVideoCount = if ((cached?.largeVideoCount ?: 0) > 0) cached!!.largeVideoCount else fresh.largeVideoCount,
                largeVideoBytes = if ((cached?.largeVideoBytes ?: 0L) > 0L) cached!!.largeVideoBytes else fresh.largeVideoBytes
            )
        }
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
            HomeEvent.FastScan -> {
                fastScan()
            }
        }
    }
}
