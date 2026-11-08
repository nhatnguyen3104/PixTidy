package com.androidtech.data.model.aduiresource

sealed class AdErrorType {
    data object NoFill: AdErrorType()
    data object NetworkError: AdErrorType()
    data object Other: AdErrorType()
    data object None: AdErrorType()
}