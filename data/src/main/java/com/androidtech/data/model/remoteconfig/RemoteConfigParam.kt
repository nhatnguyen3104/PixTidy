package com.androidtech.data.model.remoteconfig

sealed class RemoteConfigParam<T : Any> {
    abstract val key: String

    companion object {
        const val TIME_DISABLE_HIGH_FLOOR_WHEN_LOAD_FAILED = 60L
        const val TIME_DISABLE_MEDIUM_FLOOR_WHEN_LOAD_FAILED = 30L
    }

    data object ConfigAdsBannerParam: RemoteConfigParam<ConfigAdsModel>() {
        override val key: String = "config_ads_banner"
    }

    data object ConfigAdsNativeParam: RemoteConfigParam<ConfigAdsModel>() {
        override val key: String = "config_ads_native"
    }

    data object ConfigAdsAppOpenAdParam: RemoteConfigParam<ConfigAdsModel>() {
        override val key: String = "config_ads_app_open_ad"
    }

    data object ConfigAdsRewardedParam: RemoteConfigParam<ConfigAdsModel>() {
        override val key: String = "config_ads_rewarded"
    }

    data object ConfigAdsInterstitialParam: RemoteConfigParam<ConfigAdsModel>() {
        override val key: String = "config_ads_interstitial"
    }

    data object ConfigFormatAdIdParam: RemoteConfigParam<ConfigFormatAdIdModel>() {
        override val key: String = "config_format_ad_id"
    }

    data object ConfigAdsActive: RemoteConfigParam<Boolean>() {
        override val key: String = "config_ads_active"
    }

    data object ConfigCountriesDisableAds: RemoteConfigParam<List<String>>() {
        override val key: String = "country_disable_ads"
    }
}