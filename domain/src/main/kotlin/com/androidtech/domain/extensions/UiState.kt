package com.androidtech.domain.extensions

sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data class Loading(val progress: Int? = null) : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val failure: AppFailure) : UiState<Nothing>()

    val isLoading get() = this is Loading
    val isSuccess get() = this is Success
}