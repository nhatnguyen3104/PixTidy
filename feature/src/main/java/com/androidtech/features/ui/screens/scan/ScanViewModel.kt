package com.androidtech.features.ui.screens.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.model.media.ScanLayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
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
                //startScan()
            }
            is ScanEvent.CancelScan -> cancelScan()
            is ScanEvent.NavigateBack -> setEffect { ScanEffect.NavigateBack }
        }
    }

    /*private fun startScan() {
        if (scanJob?.isActive == true) return
        
        val mode = getScanMode()
        val scanFlow = when (mode) {
            ScanMode.FAST -> fastScanUseCase.execute()
            ScanMode.SMART -> smartScanUseCase.execute()
        }

        scanJob = scanFlow
            .onEach { progress ->
                setState { copy(scanProgress = progress) }
            }
            .launchIn(viewModelScope)
    }*/

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
