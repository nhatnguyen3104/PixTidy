package com.androidtech.features.ui.screens.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProgress
import com.androidtech.domain.model.media.ScanSummary
import com.androidtech.domain.model.media.SmartInsightType
import com.androidtech.domain.usecase.media.GetLatestScanResultsUseCase
import com.androidtech.domain.usecase.media.RunDeepScanUseCase
import com.androidtech.domain.usecase.media.RunSmartScanUseCase
import com.androidtech.domain.usecase.media.SaveScanResultsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds
import com.androidtech.domain.model.media.ScanProcess as DomainScanState

@HiltViewModel
class ScanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val runSmartScanUseCase: RunSmartScanUseCase,
    private val runDeepScanUseCase: RunDeepScanUseCase,
    private val getLatestScanResultsUseCase: GetLatestScanResultsUseCase,
    private val saveScanResultsUseCase: SaveScanResultsUseCase
): BaseViewModel<ScanEvent, ScanState, ScanEffect> (savedStateHandle) {

    private var scanJob: Job? = null

    private fun getScanMode(): ScanLayer {
        return savedStateHandle.get<String>("mode")?.let {
            try {
                ScanLayer.valueOf(it)
            } catch (e: Exception) {
                ScanLayer.FAST
            }
        } ?: ScanLayer.FAST
    }

    override fun createInitializeState(): ScanState = ScanState(scanLayer = getScanMode())

    override fun handleEvent(event: ScanEvent) {
        when (event) {
            is ScanEvent.StartScan -> { startScan() }
            is ScanEvent.CancelScanClick -> cancelScan()
            is ScanEvent.SmartInsightTypeClick -> { handleSmartInsightTypeClick(event.smartInsightType) }
            is ScanEvent.NavigateBackClick -> setEffect { ScanEffect.NavigateBack }
        }
    }

    private fun startScan() {
        if (scanJob?.isActive == true) return
        
        val mode = getScanMode()
        val scanFlow = when (mode) {
            ScanLayer.SMART -> runSmartScanUseCase()
            ScanLayer.DEEP -> runDeepScanUseCase()
            else -> return
        }

        scanJob = scanFlow
            .onEach {  domainState ->
                when (domainState) {
                    is DomainScanState.Idle -> {
                        setState { copy(scanProcess = DomainScanState.Idle) }
                    }
                    is DomainScanState.Scanning -> {
                        val uiProgress = DomainScanState.Scanning(
                            progress = domainState.progress
                                .copy(currentStepMessage = domainState.progress.currentStepMessage)
                        )
                        setState { copy(scanProcess = uiProgress) }
                    }
                    is DomainScanState.Success -> {
                        saveLatestResults(mode, domainState)
                        // Emit a final 100% scanning state to allow the UI to finish the gauge animation completely
                        val finalCount = domainState.summary.totalMediaCount
                        val finalProgress = ScanProgress.fixed(
                            layer = if (mode == ScanLayer.SMART) ScanLayer.SMART else ScanLayer.DEEP,
                            processed = finalCount,
                            total = finalCount,
                            message = "Scan complete!",
                            percentage = 100f
                        )
                        setState { copy(scanProcess = DomainScanState.Scanning(finalProgress)) }
                        delay(800.milliseconds)
                        
                        val uiSuccess = DomainScanState.Success(
                            insights = domainState.insights,
                            summary = domainState.summary,
                            totalReclaimableBytes = domainState.totalReclaimableBytes
                        )
                        setState { copy(scanProcess = uiSuccess) }
                    }
                    is DomainScanState.Error -> {
                        val uiError = DomainScanState.Error(
                            throwable = domainState.throwable
                        )
                        setState { copy(scanProcess = uiError) }
                    }
                }
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

    private fun saveLatestResults(mode: ScanLayer, success: DomainScanState.Success) {
        viewModelScope.launch {
            val uiState = getLatestScanResultsUseCase(Unit).firstOrNull()
            val currentSummary = (uiState as? UiState.Success)?.data
                ?: ScanSummary.EMPTY.copy(lastScanLevel = mode)

            var duplicatesBytes = 0L
            var similarityBytes = 0L
            var screenshotCount = 0
            var screenshotBytes = 0L
            var largeVideoCount = 0
            var largeVideoBytes = 0L

            success.insights.forEach { insight ->
                when (insight.type) {
                    SmartInsightType.EXACT_DUPLICATES, SmartInsightType.DUPLICATE_VIDEOS -> {
                        duplicatesBytes += insight.reclaimableBytes
                    }
                    SmartInsightType.SIMILAR_PHOTOS -> {
                        similarityBytes += insight.reclaimableBytes
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
                lastScanLevel = mode,
                duplicatesSizeBytes = duplicatesBytes,
                similaritySizeBytes = similarityBytes,
                screenshotCount = if (screenshotCount > 0) screenshotCount else currentSummary.screenshotCount,
                screenshotBytes = if (screenshotBytes > 0) screenshotBytes else currentSummary.screenshotBytes,
                largeVideoCount = if (largeVideoCount > 0) largeVideoCount else currentSummary.largeVideoCount,
                largeVideoBytes = if (largeVideoBytes > 0) largeVideoBytes else currentSummary.largeVideoBytes
            )
            
            saveScanResultsUseCase(SaveScanResultsUseCase.Params(updatedSummary, success.insights))
                .launchIn(this)
        }
    }

    private fun handleSmartInsightTypeClick(smartInsightType: SmartInsightType) {
        when (smartInsightType) {
            SmartInsightType.EXACT_DUPLICATES -> {
                setEffect { ScanEffect.NavigateToDuplicate }
            }

            SmartInsightType.SIMILAR_PHOTOS -> {
                setEffect { ScanEffect.NavigateToSimilar }
            }

            SmartInsightType.SCREENSHOTS -> {

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
