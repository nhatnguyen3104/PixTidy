package com.androidtech.base.extension

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion

/**
 * FlowEither is an extension function that converts a Flow of type T to a Flow of either
 * @param T
 * @param dispatcher
 * @return
 */
fun <T> Flow<T>.flowEither(
    dispatcher: CoroutineDispatcher = Dispatchers.IO
): Flow<Either<Failure, T>> {
    return map { t ->
        try {
            Either.Success(t)
        } catch (e: Exception) {
            Either.Failure(Failure(e))
        }
    }
        .flowOn(dispatcher)
        .catch {
            Either.Failure(Failure(it))
        }.onCompletion {
            if (it != null) {
                Either.Failure(Failure(it))
            }
        }
}