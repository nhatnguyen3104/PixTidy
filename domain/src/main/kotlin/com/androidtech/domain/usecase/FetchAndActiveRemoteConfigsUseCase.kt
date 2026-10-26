package com.androidtech.domain.usecase

import com.androidtech.domain.repository.RemoteConfigRepository
import javax.inject.Inject

class FetchAndActiveRemoteConfigsUseCase @Inject constructor(
    private val remoteConfigRepository: RemoteConfigRepository
) {
    operator fun invoke() {
        remoteConfigRepository.fetchAndActive()
    }
}