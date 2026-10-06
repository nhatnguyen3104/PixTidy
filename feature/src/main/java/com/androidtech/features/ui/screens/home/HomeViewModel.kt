package com.androidtech.features.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanSummary
import com.androidtech.domain.model.media.SmartInsightType
import com.androidtech.domain.usecase.media.GetLatestScanResultsUseCase
import com.androidtech.domain.usecase.media.RunFastScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runFastScanUseCase: RunFastScanUseCase,
    private val getLatestScanResultsUseCase: GetLatestScanResultsUseCase
) : BaseViewModel<HomeEvent, HomeState, HomeEffect>(savedStateHandle) {
    override fun createInitializeState(): HomeState = HomeState()

    private var scanJob: Job? = null

    init {
        observeScanResults()
    }

    private fun observeScanResults() {
        getLatestScanResultsUseCase(Unit)
            .filterIsInstance<UiState.Success<ScanSummary?>>()
            .onEach { state ->
                val summary = state.data ?: ScanSummary.EMPTY
                setState { copy(scanSummary = summary) }
            }
            .launchIn(viewModelScope)
    }

    private fun fastScan(hasPermission: Boolean = true) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            runFastScanUseCase(hasPermission)
                .onEach { fresh ->
                    val cached = uiState.value.scanSummary
                    val merged = if (hasPermission) {
                        fresh.copy(
                            lastScanLevel = if (cached.lastScanLevel != ScanLayer.FAST) cached.lastScanLevel else fresh.lastScanLevel,
                            duplicatesSizeBytes = cached.duplicatesSizeBytes,
                            similaritySizeBytes = cached.similaritySizeBytes,
                            screenshotCount = if (cached.screenshotCount > 0) cached.screenshotCount else fresh.screenshotCount,
                            screenshotBytes = if (cached.screenshotBytes > 0) cached.screenshotBytes else fresh.screenshotBytes,
                            largeVideoCount = if (cached.largeVideoCount > 0) cached.largeVideoCount else fresh.largeVideoCount,
                            largeVideoBytes = if (cached.largeVideoBytes > 0) cached.largeVideoBytes else fresh.largeVideoBytes
                        )
                    } else {
                        cached.copy(storageSummary = fresh.storageSummary)
                    }
                    setState { copy(scanSummary = merged) }
                }
                .collect()
        }
    }

    override fun handleEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SmartInsightTypeClick -> {
                handleSmartInsightTypeClick(event.smartInsightType)
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

            is HomeEvent.FastScan -> {
                fastScan(uiState.value.isPermissionGranted)
            }

            is HomeEvent.RequestPermissionClicked -> {
                setEffect { HomeEffect.RequestStoragePermissions }
            }

            is HomeEvent.PermissionStatusChanged -> {
                setState { copy(isPermissionGranted = event.isGranted) }
                fastScan(event.isGranted)
            }
        }
    }

    private fun handleSmartInsightTypeClick(smartInsightType: SmartInsightType) {
        when (smartInsightType) {
            SmartInsightType.EXACT_DUPLICATES -> {
                setEffect { HomeEffect.NavigateToDuplicate }
            }

            SmartInsightType.SIMILAR_PHOTOS -> {
                setEffect { HomeEffect.NavigateToSimilar }
            }

            SmartInsightType.SCREENSHOTS -> {
                setEffect { HomeEffect.NavigateToScreenshot }
            }

            SmartInsightType.LARGE_VIDEOS -> {

            }

            SmartInsightType.BLURRY_PHOTOS -> {

            }
            SmartInsightType.DEEP_OCR_DOCUMENTS -> {

            }
            SmartInsightType.DUPLICATE_VIDEOS -> {

            }
        }
    }
}
