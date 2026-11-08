package com.androidtech.domain.repository

import com.androidtech.domain.model.configad.AdPlace
import com.androidtech.domain.model.configad.ConfigAdsAppOpen
import com.androidtech.domain.model.configad.ConfigAdsBanner
import com.androidtech.domain.model.configad.ConfigAdsInterstitial
import com.androidtech.domain.model.configad.ConfigAdsNative
import com.androidtech.domain.model.configad.ConfigAdsRewarded
import com.androidtech.domain.model.configad.ConfigAdsRewardedInterstitial
import com.androidtech.domain.model.configad.ConfigFormatAdId
import com.androidtech.domain.model.configad.ConfigPreventAdClick
import kotlinx.coroutines.flow.Flow

interface RemoteConfigRepository {
    suspend fun fetchAndActive()
    fun getConfigAdsActive(): Flow<Boolean>
    fun getConfigAd(): Flow<ConfigFormatAdId?>
    fun getAdPlaceBy(adPlaceName: String): AdPlace
    fun getAdPlaces(): List<AdPlace>
    fun getConfigCountriesDisableAds(): List<String>
    fun getConfigPreventAdClick(): ConfigPreventAdClick

    fun getConfigAdsBanner(): ConfigAdsBanner

    fun getConfigAdsNative(): ConfigAdsNative

    fun getConfigAdsAppOpen(): ConfigAdsAppOpen

    fun getConfigAdsInterstitial(): ConfigAdsInterstitial

    fun getConfigAdsRewardedInterstitial(): ConfigAdsRewardedInterstitial

    fun getConfigAdsRewarded(): ConfigAdsRewarded
}