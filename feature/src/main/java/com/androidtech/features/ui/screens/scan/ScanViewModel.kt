package com.androidtech.features.ui.screens.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProcess as DomainScanState
import com.androidtech.domain.usecase.media.RunDeepSmartScanUseCase
import com.androidtech.domain.usecase.media.RunSmartScanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runSmartScanUseCase: RunSmartScanUseCase,
    private val runDeepSmartScanUseCase: RunDeepSmartScanUseCase
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
            is ScanEvent.StartScan -> {
                startScan()
            }
            is ScanEvent.CancelScan -> cancelScan()
            is ScanEvent.NavigateBack -> setEffect { ScanEffect.NavigateBack }
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
}
