package com.androidtech.data.model.mapper

import android.content.Context
import com.androidtech.data.model.configads.ConfigAdsModel
import com.androidtech.data.remoteconfig.CountryTier
import com.androidtech.data.utils.getCountryCode
import com.androidtech.data.utils.getField
import com.androidtech.domain.model.configad.ConfigAdsInterstitial

fun ConfigAdsModel.mapperInterstitialAd(context: Context, countryTier: CountryTier): ConfigAdsInterstitial {
    var isWaitLoadToShow = isWaitLoadToShow ?: false
    var maxShowPerSession = maxShowPerSession ?: ConfigAdsInterstitial.INTERSTITIAL_AD_CONFIG_MAX_SHOW_PER_SESSION
    var timePerSession = timePerSession ?: ConfigAdsInterstitial.INTERSTITIAL_AD_CONFIG_TIME_PER_SESSION
    var timeInterval = timeInterval ?: ConfigAdsInterstitial.INTERSTITIAL_AD_CONFIG_TIME_INTERVAL
    var timeIntervalAfterShowOpenAd = timeIntervalAfterShowOpenAd ?: ConfigAdsInterstitial.INTERSTITIAL_AD_CONFIG_TIME_INTERVAL_AFTER_SHOW_OPEN_AD
    var maxRetryCount = maxRetryCount ?: 0
    var delayTimesDifferentUnitId = delayTimesD?.mapNotNull { it } ?: listOf()
    var delayTimesSameUnitId = delayTimesS?.mapNotNull { it } ?: listOf()

    val tier1 = tier1
    val tier2 = tier2
    val tier3 = tier3

    val overrideAdPlaceModel: ConfigAdsModel?

    val countryAdPlaceModel = getField<ConfigAdsModel>(context.applicationContext.getCountryCode().lowercase())

    overrideAdPlaceModel = when {
        countryAdPlaceModel != null -> countryAdPlaceModel
        countryTier is CountryTier.Tier1 && tier1 != null -> tier1
        countryTier is CountryTier.Tier2 && tier2 != null -> tier2
        countryTier is CountryTier.Tier3 && tier3 != null -> tier3
        else -> null
    }

    if (overrideAdPlaceModel != null) {
        overrideAdPlaceModel.isWaitLoadToShow?.let { isWaitLoadToShow = it }
        overrideAdPlaceModel.maxShowPerSession?.let { maxShowPerSession = it }
        overrideAdPlaceModel.timePerSession?.let { timePerSession = it }
        overrideAdPlaceModel.timeInterval?.let { timeInterval = it }
        overrideAdPlaceModel.timeIntervalAfterShowOpenAd?.let { timeIntervalAfterShowOpenAd = it }
        overrideAdPlaceModel.maxRetryCount?.let { maxRetryCount = it }
        overrideAdPlaceModel.delayTimesD?.let { different ->
            delayTimesDifferentUnitId = different.mapNotNull { it }
        }
        overrideAdPlaceModel.delayTimesS?.let { same ->
            delayTimesSameUnitId = same.mapNotNull { it }
        }
    }

    return ConfigAdsInterstitial(
        isWaitLoadToShow = isWaitLoadToShow,
        maxShowPerSession = maxShowPerSession,
        timePerSession = timePerSession,
        timeInterval = timeInterval,
        timeIntervalAfterShowOpenAd = timeIntervalAfterShowOpenAd,
        maxRetryCount = maxRetryCount,
        delayTimesDifferentUnitId = delayTimesDifferentUnitId,
        delayTimesSameUnitId = delayTimesSameUnitId
    )
}