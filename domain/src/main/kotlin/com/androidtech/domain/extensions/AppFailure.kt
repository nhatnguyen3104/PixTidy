package com.androidtech.domain.extensions

sealed class AppFailure {
    data class Network(val message: String) : AppFailure()
    data class Server(val code: Int, val message: String) : AppFailure()
    data class Local(val message: String) : AppFailure()
    data class Unknown(val throwable: Throwable?) : AppFailure()
}