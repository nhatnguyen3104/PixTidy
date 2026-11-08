package com.androidtech.data.model.mapper

import android.content.Context
import com.androidtech.data.model.configads.ConfigAdsModel
import com.androidtech.data.remoteconfig.CountryTier
import com.androidtech.data.utils.getCountryCode
import com.androidtech.data.utils.getField
import com.androidtech.domain.model.configad.ConfigAdsRewarded

fun ConfigAdsModel.mapperRewardedAd(context: Context, countryTier: CountryTier): ConfigAdsRewarded {
    var isWaitLoadToShow = isWaitLoadToShow ?: false
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
        overrideAdPlaceModel.maxRetryCount?.let { maxRetryCount = it }
        overrideAdPlaceModel.delayTimesD?.let { different ->
            delayTimesDifferentUnitId = different.mapNotNull { it }
        }
        overrideAdPlaceModel.delayTimesS?.let { same ->
            delayTimesSameUnitId = same.mapNotNull { it }
        }
    }

    return ConfigAdsRewarded(
        isWaitLoadToShow = isWaitLoadToShow,
        maxRetryCount = maxRetryCount,
        delayTimesDifferentUnitId = delayTimesDifferentUnitId,
        delayTimesSameUnitId = delayTimesSameUnitId
    )
}