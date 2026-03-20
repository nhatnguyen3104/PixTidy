package com.androidtech.base.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<Event: UIEvent, State: UIState, Effect: UIEffect>(
    protected val savedStateHandle: SavedStateHandle
): ViewModel() {

    protected val currentState: State
        get() = uiState.value

    protected open val savedStateKey: String = "ui_state_key"
    protected open fun onViewModelCreated() {}
    protected open fun onViewModelClearing() {}
    protected open fun onError(throwable: Throwable) {}

    private val initState: State by lazy { createInitializeState() }

    private val _uiState = MutableStateFlow(initState)
    val uiState = _uiState
        .onEach { state ->
            savedStateToHandle(state)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000, replayExpirationMillis = 0),
            initialValue = initState
        )

    @OptIn(FlowPreview::class)
    private val _uiEvent = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_LATEST
    )

    private val uiEvent = _uiEvent
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            replay = 0
        )

    private val _uiEffect = Channel<Effect>()
    val uiEffect = _uiEffect.receiveAsFlow()

    abstract fun createInitializeState(): State

    abstract fun handleEvent(event: Event)

    init {
        restoreStateFromHandle()
        subscribeEvents()
        onViewModelCreated()
    }

    private fun subscribeEvents() {
        viewModelScope.launch {
            _uiEvent.collect { event ->
                handleEvent(event)
            }
        }
    }

    fun setEvent(event: Event) {
        _uiEvent.tryEmit(event)
    }

    fun setState(reduce: State.() -> State) {
        _uiState.update { currentState -> currentState.reduce() }
    }

    fun setEffect(builder: () -> Effect) {
        viewModelScope.launch { _uiEffect.send(builder()) }
    }

    protected open fun savedStateToHandle(state: State) {
        try {
            savedStateHandle[savedStateKey] = state
        } catch (e: Exception) {
            onError(e)
        }
    }

    protected open fun restoreStateFromHandle() {
        savedStateHandle.get<State>(savedStateKey)?.let { state ->
            _uiState.update { state }
        }
    }

    protected fun <T> getSavedStateFlow(key: String, initialValue: T): StateFlow<T> {
        return savedStateHandle.getStateFlow(key, initialValue)
    }

    protected fun <T> updateSavedState(key: String, value: T) {
        savedStateHandle[key] = value
    }

    protected fun <T> getSavedState(key: String): T? {
        return savedStateHandle.get<T>(key)
    }

    protected fun removeSavedState(key: String) {
        savedStateHandle.remove<Any>(key)
    }

    protected fun clearSavedState() {
        savedStateHandle.keys().forEach { key ->
            savedStateHandle.remove<Any>(key)
        }
    }

    override fun onCleared() {
        onViewModelClearing()
        super.onCleared()
    }
}