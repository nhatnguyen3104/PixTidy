package com.androidtech.domain.extensions

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion

/**
 * FlowEither is an extension function that converts a Flow of type T to a Flow of either
 * @param T
 * @param dispatcher
 * @return
 */
fun <T> Flow<T>.flowEither(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Flow<T> {
    return this
        .flowOn(dispatcher)
        .catch {
            Either.Failure(AppFailure.Unknown(it))
        }.onCompletion {
            if (it != null) {
                Either.Failure(AppFailure.Unknown(it))
            }
        }
}

fun <T> Flow<T>.flowUIState(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Flow<T> = this
    .flowOn(dispatcher)
    .catch {
        UiState.Error(AppFailure.Unknown(it))
    }
    .onCompletion {
        if (it != null) {
            UiState.Error(AppFailure.Unknown(it))
        }
    }