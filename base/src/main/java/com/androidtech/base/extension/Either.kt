package com.androidtech.base.extension

sealed class Either<out FAILURE, out SUCCESS> {
    data class Failure<FAILURE>(val failure: FAILURE): Either<FAILURE, Nothing>()
    data class Success<SUCCESS>(val success: SUCCESS): Either<Nothing, SUCCESS>()

    val isSuccess get() = this is Success
    val isFailure get() = this is Failure

    fun successOrNull(): SUCCESS? = (this as? Success)?.success
    fun failureOrNull(): FAILURE? = (this as? Failure)?.failure
}

class Failure(val throwable: Throwable)

inline fun <FAILURE, SUCCESS> Either<FAILURE, SUCCESS>.onSuccess(block: (SUCCESS) -> Unit): Either<FAILURE, SUCCESS> {
    if (this is Either.Success) block(success)
    return this
}

inline fun <FAILURE, SUCCESS> Either<FAILURE, SUCCESS>.onFailure(block: (FAILURE) -> Unit): Either<FAILURE, SUCCESS> {
    if (this is Either.Failure) block(failure)
    return this
}

inline fun <FAILURE, SUCCESS> Either<FAILURE, SUCCESS>.fold(
    onSuccess: (SUCCESS) -> Unit,
    onFailure: (FAILURE) -> Unit
): Any = when (this) {
    is Either.Success -> onSuccess(success)
    is Either.Failure -> onFailure(failure)
}

inline fun <FAILURE, SUCCESS, R> Either<FAILURE, SUCCESS>.fold(
    onSuccess: (SUCCESS) -> R,
    onFailure: (FAILURE) -> R
): R = when (this) {
    is Either.Success -> onSuccess(success)
    is Either.Failure -> onFailure(failure)
}