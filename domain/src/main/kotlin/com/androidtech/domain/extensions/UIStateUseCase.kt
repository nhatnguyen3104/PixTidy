package com.androidtech.domain.extensions

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

abstract class UIStateUseCase<in Param, out T> {

    abstract fun run(param: Param): Flow<UiState<T>>

    operator fun invoke(
        param: Param,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Flow<UiState<T>> = run(param).flowUIState(dispatcher)
}