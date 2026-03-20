package com.androidtech.base.extension

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

abstract class UseCase<in Param, out T> {

    abstract fun run(param: Param): Flow<T>

    operator fun invoke(
        param: Param,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): Flow<Either<Failure, T>> = run(param).flowEither(dispatcher)
}