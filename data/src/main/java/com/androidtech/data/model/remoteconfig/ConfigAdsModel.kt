package com.androidtech.data.model.remoteconfig

import com.androidtech.data.utils.Constants
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ConfigAdsModel(
    @Json(name = "is_wait_load_to_show")
    val isWaitLoadToShow: Boolean?,

    @Json(name = "max_retry_count")
    val maxRetryCount: Int?,

    @Json(name = "max_show_per_session")
    val maxShowPerSession: Int?,

    @Json(name = "time_interval")
    val timeInterval: Long?,

    @Json(name = "time_interval_after_show_open_ad")
    val timeIntervalAfterShowOpenAd: Long?
) {

    companion object {
        fun toDefaultAdsBanner(): ConfigAdsBanner = ConfigAdsBanner(
            maxRetryCount = 5,
            bannerRefreshEnable = true
        )

        fun toDefaultAdsNative(): ConfigAdsNative = ConfigAdsNative(
            maxRetryCount = 5,
            delayTimesSameUnitId = listOf()
        )

        fun toDefaultAdsInterstitial(): ConfigAdsInterstitial = ConfigAdsInterstitial(
            maxRetryCount = 5,
            isWaitLoadToShow = false,
            maxShowPerSession = Constants.INTERSTITIAL_AD_CONFIG_MAX_SHOW_PER_SESSION,
            timePerSession = Constants.INTERSTITIAL_AD_CONFIG_TIME_PER_SESSION,
            timeInterval = Constants.INTERSTITIAL_AD_CONFIG_TIME_INTERVAL,
            timeIntervalAfterShowOpenAd = Constants.INTERSTITIAL_AD_CONFIG_TIME_INTERVAL_AFTER_SHOW_OPEN_AD
        )

        fun toDefaultAdsRewarded(): ConfigAdsRewarded = ConfigAdsRewarded(
            maxRetryCount = 5,
            isWaitLoadToShow = false
        )

        fun toDefaultAdsAppOpen(): ConfigAdsAppOpen = ConfigAdsAppOpen(
            maxRetryCount = 5,
            isWaitLoadToShow = false,
            timeInterval = Constants.OPEN_APP_AD_TIME_INTERVAL
        )
    }

    fun toAdsBanner(): ConfigAdsBanner = ConfigAdsBanner(
        maxRetryCount = maxRetryCount ?: toDefaultAdsBanner().maxRetryCount,
        bannerRefreshEnable = true
    )

    fun toAdsNative(): ConfigAdsNative = ConfigAdsNative(
        maxRetryCount = maxRetryCount ?: toDefaultAdsNative().maxRetryCount,
        delayTimesSameUnitId = listOf()
    )

    fun toAdsInterstitial(): ConfigAdsInterstitial = ConfigAdsInterstitial(
        maxRetryCount = maxRetryCount ?: toDefaultAdsInterstitial().maxRetryCount,
        isWaitLoadToShow = isWaitLoadToShow ?: toDefaultAdsInterstitial().isWaitLoadToShow,
        maxShowPerSession = maxShowPerSession ?: toDefaultAdsInterstitial().maxShowPerSession,
        timePerSession = timeInterval ?: toDefaultAdsInterstitial().timePerSession,
        timeInterval = timeInterval ?: toDefaultAdsInterstitial().timeInterval,
        timeIntervalAfterShowOpenAd = timeIntervalAfterShowOpenAd ?: toDefaultAdsInterstitial().timeIntervalAfterShowOpenAd
    )

    fun toAdsRewarded(): ConfigAdsRewarded = ConfigAdsRewarded(
        maxRetryCount = maxRetryCount ?: toDefaultAdsRewarded().maxRetryCount,
        isWaitLoadToShow = isWaitLoadToShow ?: toDefaultAdsRewarded().isWaitLoadToShow
    )

    fun toAdsAppOpen(): ConfigAdsAppOpen = ConfigAdsAppOpen(
        maxRetryCount = maxRetryCount ?: toDefaultAdsAppOpen().maxRetryCount,
        isWaitLoadToShow = isWaitLoadToShow ?: toDefaultAdsAppOpen().isWaitLoadToShow,
        timeInterval = timeInterval ?: toDefaultAdsAppOpen().timeInterval
    )
}