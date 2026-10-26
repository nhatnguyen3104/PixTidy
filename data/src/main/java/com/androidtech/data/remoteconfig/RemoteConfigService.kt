package com.androidtech.data.remoteconfig

import com.androidtech.data.R
import com.androidtech.data.model.remoteconfig.ConfigAdsModel
import com.androidtech.data.model.remoteconfig.ConfigFormatAdIdModel
import com.androidtech.data.model.remoteconfig.RemoteConfigParam
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigService @Inject constructor(
    private val moshi: Moshi
) {

    private val remoteConfig by lazy {
        val settings = remoteConfigSettings {
            fetchTimeoutInSeconds = 30
            minimumFetchIntervalInSeconds = 0
        }
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(settings)
            setDefaultsAsync(R.xml.remote_configs_default)
        }
    }

    fun getConfigBannerAdsModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsBannerParam
        )
    }

    fun getConfigNativeAdsModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsNativeParam
        )
    }

    fun getConfigAppOpenAdModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsAppOpenAdParam
        )
    }

    fun getConfigRewardedAdsModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsRewardedParam
        )
    }

    fun getConfigInterstitialAdsModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsInterstitialParam
        )
    }

    fun getFormatAdIdModel(): ConfigFormatAdIdModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigFormatAdIdParam
        )
    }

    fun getRemoteConfigAdsActive(): Boolean? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsActive
        )
    }

    fun getRemoteCountriesDisableAds(): List<String>? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigCountriesDisableAds
        )
    }
}