package com.androidtech.features.ui.screens.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.usecase.scan.ScanMediaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanMediaUseCase: ScanMediaUseCase,
    savedStateHandle: SavedStateHandle,
): BaseViewModel<ScanEvent, ScanState, ScanEffect> (savedStateHandle) {

    private var scanJob: Job? = null

    override fun createInitializeState(): ScanState = ScanState()

    override fun handleEvent(event: ScanEvent) {
        when (event) {
            is ScanEvent.StartScan -> startScan()
            is ScanEvent.CancelScan -> cancelScan()
            is ScanEvent.NavigateBack -> setEffect { ScanEffect.NavigateBack }
        }
    }

    private fun startScan() {
        if (scanJob?.isActive == true) return
        
        scanJob = scanMediaUseCase.execute()
            .onEach { progress ->
                setState { copy(scanProgress = progress) }
            }
            .launchIn(viewModelScope)
    }

    private fun cancelScan() {
        viewModelScope.launch {
            scanMediaUseCase.cancel()
            scanJob?.cancel()
            setEffect { ScanEffect.NavigateBack }
        }
    }

    override fun onViewModelClearing() {
        super.onViewModelClearing()
        scanJob?.cancel()
    }
}