package com.androidtech.domain.model.configad

class ConfigPreventAdClick(
    val maxAdClickPerSession: Int,
    val timePerSession: Long,
    val timeDisableAdsWhenReachedMaxAdClick: Long
) {
    companion object {
        const val PREVENT_AD_CLICK_CONFIG_MAX_AD_CLICK_PER_SESSION = 6
        const val PREVENT_AD_CLICK_CONFIG_TIME_PER_SESSION = 120L
        const val PREVENT_AD_CLICK_DEFAULT_TIME_DISABLE = 1800L

        val default = ConfigPreventAdClick(
            maxAdClickPerSession = PREVENT_AD_CLICK_CONFIG_MAX_AD_CLICK_PER_SESSION,
            timePerSession = PREVENT_AD_CLICK_CONFIG_TIME_PER_SESSION,
            timeDisableAdsWhenReachedMaxAdClick = PREVENT_AD_CLICK_DEFAULT_TIME_DISABLE,
        )
    }
}

class ConfigAdsBanner(
    val maxRetryCount: Int,
    val delayTimesDifferentUnitId: List<Long>,
    val delayTimesSameUnitId: List<Long>,

    val bannerRefreshEnable: Boolean,
    val bannerRefreshDefaultRateInSeconds: Int,
    val bannerRefreshBufferSize: Int,
    val bannerRefreshRatesInSeconds: Map<String, Int>
) {
    companion object {
        val default = ConfigAdsBanner(
            maxRetryCount = 5,
            delayTimesDifferentUnitId = listOf(),
            delayTimesSameUnitId = listOf(),
            bannerRefreshEnable = true,
            bannerRefreshDefaultRateInSeconds = 30,
            bannerRefreshBufferSize = 1,
            bannerRefreshRatesInSeconds = mapOf(
                "admob" to 30,
                "meta" to 30,
                "iron_source" to 30,
                "liftoff" to 30,
                "pangle" to 30,
                "mintegral" to 30,
                "moloco" to 30,
                "unity" to 30
            )
        )
    }
}

class ConfigAdsNative(
    val maxRetryCount: Int,
    val delayTimesDifferentUnitId: List<Long>,
    val delayTimesSameUnitId: List<Long>,
) {
    companion object {
        val default = ConfigAdsNative(
            maxRetryCount = 0,
            delayTimesDifferentUnitId = listOf(),
            delayTimesSameUnitId = listOf()
        )
    }
}

class ConfigAdsAppOpen(
    val isWaitLoadToShow: Boolean,

    val timeInterval: Long,

    val maxRetryCount: Int,

    val delayTimesDifferentUnitId: List<Long>,

    val delayTimesSameUnitId: List<Long>
) {
    companion object {
        const val OPEN_APP_AD_TIME_INTERVAL = 1L

        val default = ConfigAdsAppOpen(
            isWaitLoadToShow = false,
            timeInterval = OPEN_APP_AD_TIME_INTERVAL,
            maxRetryCount = 0,
            delayTimesDifferentUnitId = listOf(),
            delayTimesSameUnitId = listOf()
        )
    }
}

class ConfigAdsInterstitial(
    val isWaitLoadToShow: Boolean,
    val maxShowPerSession: Int,
    val timePerSession: Long,
    val timeInterval: Long,
    val timeIntervalAfterShowOpenAd: Long,

    val maxRetryCount: Int,
    val delayTimesDifferentUnitId: List<Long>,
    val delayTimesSameUnitId: List<Long>
) {
    companion object {
        const val INTERSTITIAL_AD_CONFIG_MAX_SHOW_PER_SESSION = 10
        const val INTERSTITIAL_AD_CONFIG_TIME_PER_SESSION = 600L
        const val INTERSTITIAL_AD_CONFIG_TIME_INTERVAL = 37L
        const val INTERSTITIAL_AD_CONFIG_TIME_INTERVAL_AFTER_SHOW_OPEN_AD = 10L

        val default = ConfigAdsInterstitial(
            isWaitLoadToShow = false,
            maxShowPerSession = INTERSTITIAL_AD_CONFIG_MAX_SHOW_PER_SESSION,
            timePerSession = INTERSTITIAL_AD_CONFIG_TIME_PER_SESSION,
            timeInterval = INTERSTITIAL_AD_CONFIG_TIME_INTERVAL,
            timeIntervalAfterShowOpenAd = INTERSTITIAL_AD_CONFIG_TIME_INTERVAL_AFTER_SHOW_OPEN_AD,
            maxRetryCount = 0,
            delayTimesDifferentUnitId = listOf(),
            delayTimesSameUnitId = listOf()
        )
    }
}

class ConfigAdsRewardedInterstitial(
    val isWaitLoadToShow: Boolean,
    val maxRetryCount: Int,
    val delayTimesDifferentUnitId: List<Long>,
    val delayTimesSameUnitId: List<Long>
) {
    companion object {
        val default = ConfigAdsRewardedInterstitial(
            isWaitLoadToShow = false,
            maxRetryCount = 0,
            delayTimesDifferentUnitId = listOf(),
            delayTimesSameUnitId = listOf()
        )
    }
}

class ConfigAdsRewarded(
    val isWaitLoadToShow: Boolean,
    val maxRetryCount: Int,
    val delayTimesDifferentUnitId: List<Long>,
    val delayTimesSameUnitId: List<Long>
) {
    companion object {
        val default = ConfigAdsRewarded(
            isWaitLoadToShow = false,
            maxRetryCount = 0,
            delayTimesDifferentUnitId = listOf(),
            delayTimesSameUnitId = listOf()
        )
    }
}