package com.androidtech.data.model.configads

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ConfigPreventAdClickModel(
    @param:Json(name = "max_ad_click_per_session")
    val maxAdClickPerSession: Int?,

    @param:Json(name = "time_per_session")
    val timePerSession: Long?,

    @param:Json(name = "time_disable_ads_when_reached_max_ad_click")
    val timeDisableAdsWhenReachedMaxAdClick: Long?
)

@JsonClass(generateAdapter = true)
class ConfigAdsModel(
    @param:Json(name = "is_wait_load_to_show")
    val isWaitLoadToShow: Boolean?,

    @param:Json(name = "time_disable_v3")
    val timeDisableV3: Long?,

    @param:Json(name = "time_disable_v2")
    val timeDisableV2: Long?,

    @param:Json(name = "max_retry_count")
    val maxRetryCount: Int?,

    @param:Json(name = "delay_times_d")
    val delayTimesD: List<Long?>?,

    @param:Json(name = "delay_times_s")
    val delayTimesS: List<Long?>?,

    @param:Json(name = "max_show_per_session")
    val maxShowPerSession: Int?,

    @param:Json(name = "time_per_session")
    val timePerSession: Long?,

    @param:Json(name = "time_interval")
    val timeInterval: Long?,

    @param:Json(name = "time_interval_after_show_open_ad")
    val timeIntervalAfterShowOpenAd: Long?,

    @param:Json(name = "banner_refresh_enable")
    val bannerRefreshEnable: Boolean?,

    @param:Json(name = "banner_refresh_default_rate_s")
    val bannerRefreshDefaultRateInSeconds: Int?,

    @param:Json(name = "banner_refresh_buffer_size")
    val bannerRefreshBufferSize: Int?,

    @param:Json(name = "banner_refresh_rates_s")
    val bannerRefreshRatesInSeconds: Map<String, Int>?,

    @param:Json(name = "t1")
    val tier1: ConfigAdsModel?,

    @param:Json(name = "t2")
    val tier2: ConfigAdsModel?,

    @param:Json(name = "t3")
    val tier3: ConfigAdsModel?
)