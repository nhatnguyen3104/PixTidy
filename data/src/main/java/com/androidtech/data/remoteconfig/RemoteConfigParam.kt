package com.androidtech.data.remoteconfig

import com.androidtech.data.model.adplace.AdPlaceModel
import com.androidtech.data.model.configads.ConfigAdsModel
import com.androidtech.data.model.configads.ConfigPreventAdClickModel
import com.androidtech.data.model.formatadid.ConfigFormatAdIdModel

sealed interface RemoteConfigParam<T: Any> {
    val key: String

    data object ConfigFormatAdIdParam: RemoteConfigParam<ConfigFormatAdIdModel> {
        override val key: String = "config_format_ad_id"
    }

    data object AdPlacesAppOpenParam: RemoteConfigParam<AdPlaceModel> {
        override val key: String = "ad_places_app_open"
    }

    data object AdPlacesBannerParam: RemoteConfigParam<AdPlaceModel> {
        override val key: String = "ad_places_banner"
    }

    data object AdPlacesNativeParam: RemoteConfigParam<AdPlaceModel> {
        override val key: String = "ad_places_native"
    }

    data object AdPlacesFullScreenParam: RemoteConfigParam<AdPlaceModel> {
        override val key: String = "ad_places_fullscreen"
    }

    data object ConfigAdsBannerParam: RemoteConfigParam<ConfigAdsModel> {
        override val key: String = "config_ads_banner"
    }

    data object ConfigAdsAppOpenParam: RemoteConfigParam<ConfigAdsModel> {
        override val key: String = "config_ads_app_open"
    }

    data object ConfigAdsNativeParam: RemoteConfigParam<ConfigAdsModel> {
        override val key: String = "config_ads_native"
    }

    data object ConfigAdsInterstitialParam: RemoteConfigParam<ConfigAdsModel> {
        override val key: String = "config_ads_interstitial"
    }

    data object ConfigAdsRewardedParam: RemoteConfigParam<ConfigAdsModel> {
        override val key: String = "config_ads_rewarded"
    }

    data object ConfigAdsRewardedInterstitialParam: RemoteConfigParam<ConfigAdsModel> {
        override val key: String = "config_ads_rewarded_interstitial"
    }

    data object ConfigCountriesDisableAdsParam: RemoteConfigParam<List<String>> {
        override val key: String = "config_countries_disable_ads"
    }

    data object ConfigPreventAdClickParam: RemoteConfigParam<ConfigPreventAdClickModel> {
        override val key: String = "config_prevent_ad_click"
    }

    data object ConfigAdsActiveParam: RemoteConfigParam<Boolean> {
        override val key: String = "ads_active"
    }

}