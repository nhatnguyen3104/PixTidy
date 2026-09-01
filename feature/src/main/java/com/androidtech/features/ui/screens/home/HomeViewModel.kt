package com.androidtech.features.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.androidtech.base.viewmodel.BaseViewModel
import com.androidtech.domain.extensions.UiState
import com.androidtech.domain.usecase.storage.ObserveStorageStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeStorageStatsUseCase: ObserveStorageStatsUseCase
): BaseViewModel<HomeEvent, HomeState, HomeEffect>(savedStateHandle) {
    override fun createInitializeState(): HomeState = HomeState()

    init {
        observeStorageStats()
    }

    private fun observeStorageStats() {
        observeStorageStatsUseCase(Unit)
            .onEach { uiState ->
                when (uiState) {
                    is UiState.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is UiState.Success -> {
                        setState { copy(isLoading = false, storageStats = uiState.data) }
                    }
                    is UiState.Error -> {
                        setState { copy(isLoading = false) }
                    }
                    else -> {}
                }
            }
            .launchIn(viewModelScope)
    }

    override fun handleEvent(event: HomeEvent) {
        when(event) {
            is HomeEvent.CategoryClick -> {

            }
            is HomeEvent.ScanClicked -> {
                setEffect { HomeEffect.NavigateToScan }
            }
            is HomeEvent.SettingsClicked -> {
                setEffect { HomeEffect.NavigateToSettings }
            }
        }
    }
}