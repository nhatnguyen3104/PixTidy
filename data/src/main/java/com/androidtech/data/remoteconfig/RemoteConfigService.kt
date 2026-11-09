package com.androidtech.data.remoteconfig

import com.androidtech.data.R
import com.androidtech.data.model.adplace.AdPlaceModel
import com.androidtech.data.model.configads.ConfigAdsModel
import com.androidtech.data.model.configads.ConfigPreventAdClickModel
import com.androidtech.data.model.formatadid.ConfigFormatAdIdModel
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
    val remoteConfig by lazy {
        val settings = remoteConfigSettings {
            fetchTimeoutInSeconds = 30
            minimumFetchIntervalInSeconds = 0
        }
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(settings)
            setDefaultsAsync(R.xml.remote_configs_default)
        }
    }

    fun getFormatAdIdModel(): ConfigFormatAdIdModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigFormatAdIdParam
        )
    }

    fun getConfigBannerAdsModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsBannerParam
        )
    }

    fun getConfigAppOpenAdModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsAppOpenParam
        )
    }

    fun getConfigNativeAdsModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsNativeParam
        )
    }

    fun getConfigInterstitialAdsModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsInterstitialParam
        )
    }

    fun getConfigRewardedAdsModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsRewardedParam
        )
    }

    fun getConfigRewardedInterstitialAdsModel(): ConfigAdsModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsRewardedInterstitialParam
        )
    }

    fun getConfigCountriesDisableAds(): List<String>? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigCountriesDisableAdsParam
        )
    }

    fun getConfigPreventAdClick(): ConfigPreventAdClickModel? {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigPreventAdClickParam
        )
    }

    fun getRemoteConfigAdsActive(): Boolean {
        return remoteConfig.read(
            moshi,
            RemoteConfigParam.ConfigAdsActiveParam
        ) ?: true
    }

    fun getAdPlacesAppOpenModel(): List<AdPlaceModel> {
        return remoteConfig.readList(
            moshi,
            RemoteConfigParam.AdPlacesAppOpenParam
        )
    }

    fun getAdPlacesBannerModel(): List<AdPlaceModel> {
        return remoteConfig.readList(
            moshi,
            RemoteConfigParam.AdPlacesBannerParam
        )
    }

    fun getAdPlacesNativeModel(): List<AdPlaceModel> {
        return remoteConfig.readList(
            moshi,
            RemoteConfigParam.AdPlacesNativeParam
        )
    }

    fun getAdPlacesFullScreenModel(): List<AdPlaceModel> {
        return remoteConfig.readList(
            moshi,
            RemoteConfigParam.AdPlacesFullScreenParam
        )
    }
}
