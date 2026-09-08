package com.androidtech.features.ui.screens.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProcess as DomainScanState
import com.androidtech.domain.model.media.SmartInsightType
import com.androidtech.domain.usecase.media.GetLatestScanResultsUseCase
import com.androidtech.domain.usecase.media.RunDeepSmartScanUseCase
import com.androidtech.domain.usecase.media.RunSmartScanUseCase
import com.androidtech.domain.usecase.media.SaveScanResultsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runSmartScanUseCase: RunSmartScanUseCase,
    private val runDeepSmartScanUseCase: RunDeepSmartScanUseCase,
    private val getLatestScanResultsUseCase: GetLatestScanResultsUseCase,
    private val saveScanResultsUseCase: SaveScanResultsUseCase
): BaseViewModel<ScanEvent, ScanState, ScanEffect> (savedStateHandle) {

    private var scanJob: Job? = null

    private fun getScanMode(): ScanLayer {
        return savedStateHandle.get<String>("mode")?.let {
            try {
                ScanLayer.valueOf(it)
            } catch (e: Exception) {
                ScanLayer.FAST_SCAN
            }
        } ?: ScanLayer.FAST_SCAN
    }

    override fun createInitializeState(): ScanState = ScanState(scanLayer = getScanMode())

    override fun handleEvent(event: ScanEvent) {
        when (event) {
            is ScanEvent.StartScanClick -> {
                startScan()
            }
            is ScanEvent.CancelScanClick -> cancelScan()
            is ScanEvent.NavigateBackClick -> setEffect { ScanEffect.NavigateBack }
        }
    }

    private fun startScan() {
        if (scanJob?.isActive == true) return
        
        val mode = getScanMode()
        val scanFlow = when (mode) {
            ScanLayer.SMART_SCAN -> runSmartScanUseCase()
            ScanLayer.DEEP_SMART_SCAN -> runDeepSmartScanUseCase()
            else -> return
        }

        scanJob = scanFlow
            .onEach {  domainState ->
                val uiProgress = when (domainState) {
                    is DomainScanState.Idle -> DomainScanState.Idle
                    is DomainScanState.Scanning -> {
                        DomainScanState.Scanning(
                            progress = domainState.progress
                                .copy(currentStepMessage = domainState.progress.currentStepMessage)
                        )
                    }
                    is DomainScanState.Success -> {
                        saveLatestResults(domainState)
                        DomainScanState.Success(
                            insights = domainState.insights,
                            totalReclaimableBytes = domainState.totalReclaimableBytes
                        )
                    }
                    is DomainScanState.Error -> {
                        DomainScanState.Error(
                            throwable = domainState.throwable
                        )
                    }
                }
                setState { copy(scanProcess = uiProgress) }
            }
            .launchIn(viewModelScope)
    }

    private fun cancelScan() {
        viewModelScope.launch {
            scanJob?.cancel()
            setEffect { ScanEffect.NavigateBack }
        }
    }

    override fun onViewModelClearing() {
        super.onViewModelClearing()
        scanJob?.cancel()
    }

    private fun saveLatestResults(success: DomainScanState.Success) {
        viewModelScope.launch {
            val currentSummary = getLatestScanResultsUseCase().firstOrNull() 
                ?: com.androidtech.domain.model.media.FastScanSummary.EMPTY

            var duplicatesBytes = 0L
            var screenshotCount = 0
            var screenshotBytes = 0L
            var largeVideoCount = 0
            var largeVideoBytes = 0L

            success.insights.forEach { insight ->
                when (insight.type) {
                    SmartInsightType.EXACT_DUPLICATES -> {
                        duplicatesBytes += insight.reclaimableBytes
                    }
                    SmartInsightType.SIMILAR_PHOTOS -> {
                        duplicatesBytes += insight.reclaimableBytes
                    }
                    SmartInsightType.SCREENSHOTS -> {
                        screenshotCount = insight.affectedItemsCount
                        screenshotBytes = insight.reclaimableBytes
                    }
                    SmartInsightType.LARGE_VIDEOS -> {
                        largeVideoCount = insight.affectedItemsCount
                        largeVideoBytes = insight.reclaimableBytes
                    }
                    else -> {}
                }
            }

            val updatedSummary = currentSummary.copy(
                duplicatesSizeBytes = duplicatesBytes,
                screenshotCount = if (screenshotCount > 0) screenshotCount else currentSummary.screenshotCount,
                screenshotBytes = if (screenshotBytes > 0) screenshotBytes else currentSummary.screenshotBytes,
                largeVideoCount = if (largeVideoCount > 0) largeVideoCount else currentSummary.largeVideoCount,
                largeVideoBytes = if (largeVideoBytes > 0) largeVideoBytes else currentSummary.largeVideoBytes
            )
            
            saveScanResultsUseCase(updatedSummary, success.insights)
        }
    }
}
