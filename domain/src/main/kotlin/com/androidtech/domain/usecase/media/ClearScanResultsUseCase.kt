package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.AppFailure
import com.androidtech.domain.extensions.Either
import com.androidtech.domain.extensions.UseCase
import com.androidtech.domain.repository.ScanResultsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ClearScanResultsUseCase @Inject constructor(
    private val scanResultsRepository: ScanResultsRepository
) : UseCase<Unit, Unit>() {
    override fun run(param: Unit): Flow<Either<AppFailure, Unit>> = flow {
        scanResultsRepository.clearResults()
        emit(Either.Success(Unit))
    }
}
