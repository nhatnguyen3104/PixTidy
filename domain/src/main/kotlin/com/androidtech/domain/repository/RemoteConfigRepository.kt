package com.androidtech.domain.repository

import com.androidtech.domain.model.configad.ConfigFormatAdId
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    fun fetchAndActive()
    fun getConfigAdsActive(): Flow<Boolean>
    fun getConfigAd(): Flow<ConfigFormatAdId?>
}