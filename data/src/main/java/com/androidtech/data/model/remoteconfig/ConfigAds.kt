package com.androidtech.data.model.remoteconfig

class ConfigPreventAdClick (
    val maxAdClickPerSession: Int,

    val timePerSession: Long,

    val timeDisableAdsWhenReachedMaxAdClick: Long,
)

class ConfigAdsBanner (
    val maxRetryCount: Int,

    val bannerRefreshEnable: Boolean,

   /* val delayTimesDifferentUnitId: List<Long>,

    val delayTimesSameUnitId: List<Long>,

    val timeDisableHighFloorPriceWhenLoadFailed: Long,

    val timeDisableMediumFloorPriceWhenLoadFailed: Long,

    val bannerCollapsibleTimeInterval: Long,

    val bannerEnableLargeAnchored: Boolean,


    val bannerRefreshDefaultRateInSeconds: Int,

    val bannerRefreshBufferSize: Int,

    val bannerRefreshRatesInSeconds: Map<String, Int>,*/
)

class ConfigAdsNative (
    val maxRetryCount: Int,
    val delayTimesSameUnitId: List<Long>,

    /*val delayTimesDifferentUnitId: List<Long>,
    val timeDisableHighFloorPriceWhenLoadFailed: Long,
    val timeDisableMediumFloorPriceWhenLoadFailed: Long,*/
)


class ConfigAdsAppOpen (
    val isWaitLoadToShow: Boolean,

    val timeInterval: Long,

    val maxRetryCount: Int,

    /*val delayTimesDifferentUnitId: List<Long>,

    val delayTimesSameUnitId: List<Long>,

    val timeDisableHighFloorPriceWhenLoadFailed: Long,

    val timeDisableMediumFloorPriceWhenLoadFailed: Long,*/
)

class ConfigAdsInterstitial (
    val maxRetryCount: Int,
    val timeIntervalAfterShowOpenAd: Long,
    val isWaitLoadToShow: Boolean,

    val maxShowPerSession: Int,

    val timePerSession: Long,

    val timeInterval: Long,

    /*val delayTimesDifferentUnitId: List<Long>,

    val delayTimesSameUnitId: List<Long>,

    val timeDisableHighFloorPriceWhenLoadFailed: Long,

    val timeDisableMediumFloorPriceWhenLoadFailed: Long,*/
)

class ConfigAdsRewardedInterstitial (
    val isWaitLoadToShow: Boolean,

    val maxRetryCount: Int,

   /* val delayTimesDifferentUnitId: List<Long>,

    val delayTimesSameUnitId: List<Long>,

    val timeDisableHighFloorPriceWhenLoadFailed: Long,

    val timeDisableMediumFloorPriceWhenLoadFailed: Long,*/
)

class ConfigAdsRewarded (
    val isWaitLoadToShow: Boolean,

    val maxRetryCount: Int,

    /*val delayTimesDifferentUnitId: List<Long>,

    val delayTimesSameUnitId: List<Long>,

    val timeDisableHighFloorPriceWhenLoadFailed: Long,

    val timeDisableMediumFloorPriceWhenLoadFailed: Long,*/
)