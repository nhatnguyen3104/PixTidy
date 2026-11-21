package com.androidtech.features.ui.screens.scan

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ads.monetization.AdManager
import com.ads.monetization.adplacename.AdPlaceName
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.data.model.aduiresource.getAdPlaceName
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanProgress
import com.androidtech.domain.model.media.SmartInsightType
import com.androidtech.domain.usecase.media.RunDeepScanUseCase
import com.androidtech.domain.usecase.media.RunSmartScanUseCase
import com.androidtech.domain.usecase.media.SaveScanResultsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val saveScanResultsUseCase: SaveScanResultsUseCase,
    private val adManager: AdManager
): BaseViewModel<ScanEvent, ScanState, ScanEffect> (savedStateHandle) {

    private var scanJob: Job? = null

    init {
        loadBannerAd()
        observeBannerAd()
    }

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
                        saveLatestResults(domainState)
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

    private fun saveLatestResults(success: DomainScanState.Success) {
        viewModelScope.launch {
            saveScanResultsUseCase(SaveScanResultsUseCase.Params(success.summary, success.insights))
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
                setEffect { ScanEffect.NavigareToScreenshot }
            }

            SmartInsightType.LARGE_VIDEOS -> {
                setEffect { ScanEffect.NavigateToLargeVideo }
            }

            SmartInsightType.BLURRY_PHOTOS -> {
                setEffect { ScanEffect.NavigateToBlurry }
            }
            SmartInsightType.DEEP_OCR_DOCUMENTS -> {
                setEffect { ScanEffect.NavigateToOcrDocument }
            }
            SmartInsightType.DUPLICATE_VIDEOS -> {

            }
        }
    }

    private fun loadBannerAd() {
        viewModelScope.launch {
            adManager.preloadBannerNativeAd(
                placeName = AdPlaceName.SCAN_BOTTOM,
                subPlaceLevel = 0
            )
            val bannerHolder = adManager.getBannerAdHolder(AdPlaceName.SCAN_BOTTOM)
            setState { copy(bannerAdHolder = bannerHolder) }
        }
    }

    private fun observeBannerAd() {
        viewModelScope.launch {
            adManager.adBannerNativeFlow.collect { resource ->
                if (resource.getAdPlaceName() == AdPlaceName.SCAN_BOTTOM) {
                    val bannerHolder = adManager.getBannerAdHolder(AdPlaceName.SCAN_BOTTOM)
                    setState { copy(bannerAdHolder = bannerHolder) }
                }
            }
        }
    }

}
