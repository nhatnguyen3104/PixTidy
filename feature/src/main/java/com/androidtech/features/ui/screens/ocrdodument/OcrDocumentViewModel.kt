package com.androidtech.features.ui.screens.ocrdodument

import android.app.PendingIntent
import android.content.IntentSender
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ads.monetization.AdManager
import com.ads.monetization.adplacename.AdPlaceName
import com.androidtech.base.R
import com.androidtech.base.utils.UiText
import com.androidtech.base.utils.formatSize
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.data.model.aduiresource.getAdPlaceName
import com.androidtech.data.repository.RequiresUserConfirmationException
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.RequiresUserConsentException
import com.androidtech.domain.usecase.media.GetLatestOcrDocumentUseCase
import com.androidtech.domain.usecase.media.PerformSafeCleanupUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OcrDocumentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getLatestOcrDocumentUseCase: GetLatestOcrDocumentUseCase,
    private val performSafeCleanupUseCase: PerformSafeCleanupUseCase,
    private val adManager: AdManager
): BaseViewModel<OcrDocumentEvent, OcrDocumentState, OcrDocumentEffect>(savedStateHandle) {
    override fun createInitializeState(): OcrDocumentState = OcrDocumentState()

    init {
        loadOcrDocument()
    }

    private fun loadOcrDocument() {
        getLatestOcrDocumentUseCase(Unit)
            .onEach { uiState ->
                when (uiState) {
                    is UiState.Loading -> setState { copy(isLoading = true) }
                    is UiState.Success -> setState {
                        copy(
                            isLoading = false,
                            ocrDocumentLists = uiState.data
                        )
                    }
                    is UiState.Error -> setState { copy(isLoading = false) }
                    else -> {}
                }
            }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: OcrDocumentEvent) {
        when (event) {
            is OcrDocumentEvent.SelectAllClicked -> {
                setAllSelection(true)
            }
            is OcrDocumentEvent.ClearSelectionClicked -> {
                setAllSelection(false)
            }
            is OcrDocumentEvent.NavigateBackClick -> {
                handleBack()
            }
            is OcrDocumentEvent.DeleteClick -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    setEffect { OcrDocumentEffect.RequestWritePermission(event.selectedPhotoIds) }
                } else {
                    deletePhotos(event.selectedPhotoIds)
                }
            }
            is OcrDocumentEvent.PhotoClicked -> {
                previewPhoto(event.photoId)
            }
            is OcrDocumentEvent.ClosePreview -> closePreview()

            is OcrDocumentEvent.PhotoToggleSelection -> {
                togglePhotoSelection(event.photoId)
            }
            is OcrDocumentEvent.ConfirmCacheCleanup -> {
                confirmCacheCleanup(event.photoIds)
            }
            is OcrDocumentEvent.DismissBackConfirmation -> {
                setState { copy(showBackConfirmationDialog = false) }
            }
            is OcrDocumentEvent.ConfirmBackClicked -> {
                setState { copy(showBackConfirmationDialog = false) }
                setEffect { OcrDocumentEffect.NavigateBack }
            }
        }
    }

    private fun confirmCacheCleanup(photoIds: List<Long>) {
        viewModelScope.launch {
            val itemsToRemove = mutableListOf<MediaItem>()
            uiState.value.ocrDocumentLists.forEach { item ->
                if (item.id in photoIds) itemsToRemove.add(item)
            }
            performSafeCleanupUseCase.updateCacheOnly(itemsToRemove)
        }
    }

    private fun deletePhotos(selectedPhotoIds: List<Long>) {
        if (selectedPhotoIds.isEmpty()) return

        viewModelScope.launch {
            setState { copy(isLoading = true) }

            // Find MediaItems with ID selected
            val itemsToDelete = mutableListOf<MediaItem>()
            uiState.value.ocrDocumentLists.forEach { item ->
                if (item.id in selectedPhotoIds) itemsToDelete.add(item)
            }

            performSafeCleanupUseCase(itemsToDelete, confirmed = true)
                .onSuccess { result ->
                    setState { copy(isLoading = false) }
                    val message = UiText.StringResource(R.string.text_notification_clean, result.itemsDeletedCount, result.bytesReclaimed.formatSize())
                    setEffect { OcrDocumentEffect.ShowSnackbar(message) }
                }
                .onFailure { error ->
                    setState { copy(isLoading = false) }
                    val intentSender = when (error) {
                        is RequiresUserConfirmationException -> error.intentSender
                        is RequiresUserConsentException -> {
                            (error.actionSender as? PendingIntent)?.intentSender
                                ?: (error.actionSender as? IntentSender)
                        }
                        else -> null
                    }
                    if (intentSender != null) {
                        setEffect { OcrDocumentEffect.LaunchIntentSender(intentSender) }
                    } else {
                        setEffect { OcrDocumentEffect.ShowSnackbar(UiText.StringResource(R.string.text_error_clean)) }
                    }
                }
        }
    }

    private fun togglePhotoSelection(photoId: Long) {
        val currentItems = uiState.value.ocrDocumentLists
        val updatedItems = currentItems.map { item ->
            if(item.id == photoId) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
        setState { copy(ocrDocumentLists = updatedItems) }
    }

    private fun previewPhoto(itemId: Long) {
        val items = uiState.value.ocrDocumentLists
        val index = items.indexOfFirst { it.id == itemId }
        if (index != -1) {
            setState {
                copy(
                    previewMediaList = items,
                    previewInitialIndex = index
                )
            }
        }
    }

    private fun closePreview() {
        setState { copy(previewMediaList = null) }
    }

    private fun setAllSelection(isSelected: Boolean) {
        val currentGroups = uiState.value.ocrDocumentLists
        val updatedGroups = currentGroups.map { item ->
            item.copy(isSelected = isSelected)
        }
        setState { copy(ocrDocumentLists = updatedGroups) }
    }

    private fun handleBack() {
        val hasSelectedItems = currentState.ocrDocumentLists.any { it.isSelected }

        if (hasSelectedItems) {
            setState { copy(showBackConfirmationDialog = true) }
        } else {
            setEffect { OcrDocumentEffect.NavigateBack }
        }
    }

    private fun loadBannerAd() {
        viewModelScope.launch {
            adManager.preloadBannerNativeAd(
                placeName = AdPlaceName.OCR_DOCUMENT_BOTTOM,
                subPlaceLevel = 0
            )
            val bannerHolder = adManager.getBannerAdHolder(AdPlaceName.OCR_DOCUMENT_BOTTOM)
            setState { copy(bannerAdHolder = bannerHolder) }
        }
    }

    private fun observeBannerAd() {
        viewModelScope.launch {
            adManager.adBannerNativeFlow.collect { resource ->
                if (resource.getAdPlaceName() == AdPlaceName.OCR_DOCUMENT_BOTTOM) {
                    val bannerHolder = adManager.getBannerAdHolder(AdPlaceName.OCR_DOCUMENT_BOTTOM)
                    setState { copy(bannerAdHolder = bannerHolder) }
                }
            }
        }
    }
}