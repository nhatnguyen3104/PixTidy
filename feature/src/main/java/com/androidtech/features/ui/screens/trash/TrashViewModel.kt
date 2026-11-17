package com.androidtech.features.ui.screens.trash

import android.app.PendingIntent
import android.content.IntentSender
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.ads.monetization.AdManager
import com.ads.monetization.adplacename.AdPlaceName
import com.androidtech.base.R
import com.androidtech.base.utils.UiText
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.data.model.aduiresource.getAdPlaceName
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.model.media.TrashActionResult
import com.androidtech.domain.usecase.media.DeletePermanentlyUseCase
import com.androidtech.domain.usecase.media.GetTrashedMediaUseCase
import com.androidtech.domain.usecase.media.RestoreItemsUseCase
import com.androidtech.features.ui.screens.similar.SimilarEffect
import com.androidtech.features.ui.screens.similar.SimilarEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getTrashedMediaUseCase: GetTrashedMediaUseCase,
    private val restoreItemsUseCase: RestoreItemsUseCase,
    private val deletePermanentlyUseCase: DeletePermanentlyUseCase,
    private val adManager: AdManager
): BaseViewModel<TrashEvent, TrashState, TrashEffect>(savedStateHandle) {
    override fun createInitializeState(): TrashState = TrashState()

    init {
        loadTrashMedia()
        loadBannerAd()
        observeBannerAd()
    }

    private fun loadTrashMedia() {
        getTrashedMediaUseCase(Unit).onEach { result ->
            when (result) {
                is UiState.Loading -> setState { copy(isLoading = true) }
                is UiState.Success -> setState {
                    copy(
                        isLoading = false,
                        trashMedia = result.data
                    )
                }
                is UiState.Error -> setState { copy(isLoading = false) }
                else -> {}
            }
        }.launchIn(viewModelScope)
    }

    override fun handleEvent(event: TrashEvent) {
        when (event) {
            is TrashEvent.SelectAllClicked -> {
                setAllSelection(true)
            }
            is TrashEvent.ClearSelectionClicked -> {
                setAllSelection(false)
            }
            is TrashEvent.OnItemClick -> {
                previewPhoto(event.id)
            }
            is TrashEvent.ToggleItem -> {
                toggleItemSelection(event.id)
            }
            is TrashEvent.RestoreClick -> {
                restoreItems(event.selectedIds)
            }
            is TrashEvent.DeleteClick -> {
                deleteItems(event.selectedIds)
            }
            is TrashEvent.NavigateBackClick -> {
                handleBack()
            }
            is TrashEvent.ClosePreview -> {
                closePreview()
            }
            is TrashEvent.DismissBackConfirmation -> {
                setState { copy(showBackConfirmationDialog = false) }
            }
            is TrashEvent.ConfirmBackClicked -> {
                setState { copy(showBackConfirmationDialog = false) }
                setEffect { TrashEffect.NavigateBack }
            }
        }
    }

    private fun toggleItemSelection(itemId: Long) {
        val items = uiState.value.trashMedia
        val updatedItems = items.map { item ->
            if (item.id == itemId) {
                item.copy(isSelected = !item.isSelected)
            } else {
                item
            }
        }
        setState { copy(trashMedia = updatedItems) }
    }

    private fun restoreItems(selectedItems: List<Long>) {
        if (selectedItems.isEmpty()) return
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            when (val result = restoreItemsUseCase(selectedItems)) {
                is TrashActionResult.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect { TrashEffect.ShowSnackbar(UiText.StringResource(R.string.text_notification_clean, selectedItems.size, "")) }
                }
                is TrashActionResult.RequiresUserConsent -> {
                    setState { copy(isLoading = false) }
                    val intentSender = (result.actionSender as? PendingIntent)?.intentSender
                        ?: (result.actionSender as? IntentSender)
                    if (intentSender != null) {
                        setEffect { TrashEffect.LaunchIntentSender(intentSender) }
                    } else {
                        setEffect { TrashEffect.ShowSnackbar(UiText.StringResource(R.string.text_error_clean)) }
                    }
                }
                is TrashActionResult.Error -> {
                    setState { copy(isLoading = false) }
                    setEffect { TrashEffect.ShowSnackbar(UiText.StringResource(R.string.text_error_clean)) }
                }
            }
        }
    }

    private fun deleteItems(selectedItems: List<Long>) {
        if (selectedItems.isEmpty()) return
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            when (val result = deletePermanentlyUseCase(selectedItems)) {
                is TrashActionResult.Success -> {
                    setState { copy(isLoading = false) }
                    setEffect { TrashEffect.ShowSnackbar(UiText.StringResource(R.string.text_notification_clean, selectedItems.size, "")) }
                }
                is TrashActionResult.RequiresUserConsent -> {
                    setState { copy(isLoading = false) }
                    val intentSender = (result.actionSender as? PendingIntent)?.intentSender
                        ?: (result.actionSender as? IntentSender)
                    if (intentSender != null) {
                        setEffect { TrashEffect.LaunchIntentSender(intentSender) }
                    } else {
                        setEffect { TrashEffect.ShowSnackbar(UiText.StringResource(R.string.text_error_clean)) }
                    }
                }
                is TrashActionResult.Error -> {
                    setState { copy(isLoading = false) }
                    setEffect { TrashEffect.ShowSnackbar(UiText.StringResource(R.string.text_error_clean)) }
                }
            }
        }
    }

    private fun previewPhoto(itemId: Long) {
        val items = uiState.value.trashMedia
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
        val currentItems = uiState.value.trashMedia
        val updatedItems = currentItems.map { item ->
            item.copy(isSelected = isSelected)
        }
        setState { copy(trashMedia = updatedItems) }
    }

    private fun handleBack() {
        val hasSelectedItems = currentState.trashMedia.any { it.isSelected }
        if (hasSelectedItems) {
            setState { copy(showBackConfirmationDialog = true) }
        } else {
            setEffect { TrashEffect.NavigateBack }
        }
    }

    private fun loadBannerAd() {
        viewModelScope.launch {
            adManager.preloadBannerNativeAd(
                placeName = AdPlaceName.TRASH_BOTTOM,
                subPlaceLevel = 0
            )
            val bannerHolder = adManager.getBannerAdHolder(AdPlaceName.TRASH_BOTTOM)
            setState { copy(bannerAdHolder = bannerHolder) }
        }
    }

    private fun observeBannerAd() {
        viewModelScope.launch {
            adManager.adBannerNativeFlow.collect { resource ->
                if (resource.getAdPlaceName() == AdPlaceName.TRASH_BOTTOM) {
                    val bannerHolder = adManager.getBannerAdHolder(AdPlaceName.TRASH_BOTTOM)
                    setState { copy(bannerAdHolder = bannerHolder) }
                }
            }
        }
    }
}
