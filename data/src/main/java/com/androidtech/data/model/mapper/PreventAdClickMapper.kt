package com.androidtech.data.model.mapper

import com.androidtech.data.model.configads.ConfigPreventAdClickModel
import com.androidtech.domain.model.configad.ConfigPreventAdClick
import com.androidtech.domain.model.configad.ConfigPreventAdClick.Companion.PREVENT_AD_CLICK_CONFIG_MAX_AD_CLICK_PER_SESSION
import com.androidtech.domain.model.configad.ConfigPreventAdClick.Companion.PREVENT_AD_CLICK_CONFIG_TIME_PER_SESSION
import com.androidtech.domain.model.configad.ConfigPreventAdClick.Companion.PREVENT_AD_CLICK_DEFAULT_TIME_DISABLE

fun ConfigPreventAdClickModel.mapperModel(): ConfigPreventAdClick {
    return ConfigPreventAdClick(
        maxAdClickPerSession = maxAdClickPerSession ?: PREVENT_AD_CLICK_CONFIG_MAX_AD_CLICK_PER_SESSION,
        timePerSession = timePerSession ?: PREVENT_AD_CLICK_CONFIG_TIME_PER_SESSION,
        timeDisableAdsWhenReachedMaxAdClick = timeDisableAdsWhenReachedMaxAdClick ?: PREVENT_AD_CLICK_DEFAULT_TIME_DISABLE
    )
}