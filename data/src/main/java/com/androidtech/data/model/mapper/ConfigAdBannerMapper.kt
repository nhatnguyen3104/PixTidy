package com.androidtech.data.model.mapper

import android.content.Context
import com.androidtech.data.model.configads.ConfigAdsModel
import com.androidtech.data.remoteconfig.CountryTier
import com.androidtech.data.utils.getCountryCode
import com.androidtech.data.utils.getDeviceLanguageCodes
import com.androidtech.data.utils.getField
import com.androidtech.domain.model.configad.ConfigAdsBanner

fun ConfigAdsModel.mapperBannerAd(context: Context, countryTier: CountryTier): ConfigAdsBanner {
    var maxRetryCount = maxRetryCount ?: ConfigAdsBanner.default.maxRetryCount
    var delayTimesDifferentUnitId = delayTimesD?.mapNotNull { it } ?: ConfigAdsBanner.default.delayTimesDifferentUnitId
    var delayTimesSameUnitId = delayTimesS?.mapNotNull { it } ?: ConfigAdsBanner.default.delayTimesSameUnitId
    var bannerRefreshEnable = bannerRefreshEnable ?: ConfigAdsBanner.default.bannerRefreshEnable
    var bannerRefreshDefaultRateInSeconds = bannerRefreshDefaultRateInSeconds ?: ConfigAdsBanner.default.bannerRefreshDefaultRateInSeconds
    var bannerRefreshBufferSize = bannerRefreshBufferSize ?: ConfigAdsBanner.default.bannerRefreshBufferSize
    var bannerRefreshRatesInSeconds = bannerRefreshRatesInSeconds ?: ConfigAdsBanner.default.bannerRefreshRatesInSeconds

    val overrideAdPlaceModel: ConfigAdsModel?

    val countryAdPlaceModelInVn = getField<ConfigAdsModel>("vn")
    val countryAdPlaceModel = if (countryAdPlaceModelInVn != null && (getDeviceLanguageCodes().contains("vi") || context.applicationContext.getCountryCode().lowercase() == "vn")) {
        countryAdPlaceModelInVn
    } else {
        getField<ConfigAdsModel>(context.applicationContext.getCountryCode().lowercase())
    }

    overrideAdPlaceModel = when {
        countryAdPlaceModel != null -> countryAdPlaceModel
        countryTier is CountryTier.Tier1 && tier1 != null -> tier1
        countryTier is CountryTier.Tier2 && tier2 != null -> tier2
        countryTier is CountryTier.Tier3 && tier3 != null -> tier3
        else -> null
    }

    if (overrideAdPlaceModel != null) {
        overrideAdPlaceModel.maxRetryCount?.let { maxRetryCount = it }
        overrideAdPlaceModel.delayTimesD?.let { different ->
            delayTimesDifferentUnitId = different.mapNotNull { it }
        }
        overrideAdPlaceModel.delayTimesS?.let { same ->
            delayTimesSameUnitId = same.mapNotNull { it }
        }
        overrideAdPlaceModel.bannerRefreshEnable?.let { bannerRefreshEnable = it }
        overrideAdPlaceModel.bannerRefreshDefaultRateInSeconds?.let { bannerRefreshDefaultRateInSeconds = it }
        overrideAdPlaceModel.bannerRefreshBufferSize?.let { bannerRefreshBufferSize = it }
        overrideAdPlaceModel.bannerRefreshRatesInSeconds?.let { bannerRefreshRatesInSeconds = it }
    }

    return ConfigAdsBanner(
        maxRetryCount = maxRetryCount,
        delayTimesDifferentUnitId = delayTimesDifferentUnitId,
        delayTimesSameUnitId = delayTimesSameUnitId,
        bannerRefreshEnable = bannerRefreshEnable,
        bannerRefreshDefaultRateInSeconds = bannerRefreshDefaultRateInSeconds,
        bannerRefreshBufferSize = bannerRefreshBufferSize,
        bannerRefreshRatesInSeconds = bannerRefreshRatesInSeconds
    )
}