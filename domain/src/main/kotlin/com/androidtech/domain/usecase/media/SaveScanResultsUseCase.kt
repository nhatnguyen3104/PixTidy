package com.androidtech.domain.usecase.media

import com.androidtech.domain.extensions.AppFailure
import com.androidtech.domain.extensions.Either
import com.androidtech.domain.extensions.UseCase
import com.androidtech.domain.model.media.FastScanSummary
import com.androidtech.domain.model.media.SmartInsight
import com.androidtech.domain.repository.ScanResultsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class SaveScanResultsUseCase @Inject constructor(
    private val scanResultsRepository: ScanResultsRepository
) : UseCase<SaveScanResultsUseCase.Params, Unit>() {

    data class Params(val summary: FastScanSummary, val insights: List<SmartInsight>)

    override fun run(param: Params): Flow<Either<AppFailure, Unit>> = flow {
        scanResultsRepository.saveResults(param.summary, param.insights)
        emit(Either.Success(Unit))
    }
}
