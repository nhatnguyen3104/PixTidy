package com.androidtech.data.model.formatadid

import android.content.Context
import com.androidtech.data.remoteconfig.CountryTier
import com.androidtech.data.utils.getCountryCode
import com.androidtech.data.utils.getDeviceLanguageCodes
import com.androidtech.data.utils.getField
import com.androidtech.domain.model.configad.ConfigAdId
import com.androidtech.domain.model.configad.ConfigFormatAdId
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import dagger.hilt.android.qualifiers.ApplicationContext

@JsonClass(generateAdapter = true)
class ConfigAdIdModel(
    @param:Json(name = "adUnitId") val adUnitId: String?,
    @param:Json(name = "v1") val v1: String?,
    @param:Json(name = "v2") val v2: String?,
    @param:Json(name = "v3") val v3: String?
)

@JsonClass(generateAdapter = true)
class ConfigFormatAdIdModel(
    @param:Json(name = "interstitial") val inter: ConfigAdIdModel?,
    @param:Json(name = "app_open") val appOpen: ConfigAdIdModel?,
    @param:Json(name = "native_small") val nativeSmall: ConfigAdIdModel?,
    @param:Json(name = "native_medium") val nativeMedium: ConfigAdIdModel?,
    @param:Json(name = "banner") val banner: ConfigAdIdModel?,
    @param:Json(name = "rewarded") val rewarded: ConfigAdIdModel?,
    @param:Json(name = "rewarded_interstitial") val rewardedInter: ConfigAdIdModel?,
    @param:Json(name = "t1") val tier1: ConfigFormatAdIdModel?,
    @param:Json(name = "t2") val tier2: ConfigFormatAdIdModel?,
    @param:Json(name = "t3") val tier3: ConfigFormatAdIdModel?,
) {
    companion object {
        val default =
            ConfigFormatAdIdModel(null, null, null, null, null, null, null, null, null, null)
    }
}

fun ConfigAdIdModel.mapperData(): ConfigAdId {
    return ConfigAdId(
        adUnitId = adUnitId ?: "",
        allPrice = v1 ?: adUnitId ?: "",
        mediumPrice = v2 ?: "",
        highPrice = v3 ?: ""
    )
}

fun ConfigFormatAdIdModel.mapperData(
    context: Context,
    countryTier: CountryTier
): ConfigFormatAdId {
    var banner = banner?.mapperData()
    var nativeSmall = nativeSmall?.mapperData()
    var nativeMedium = nativeMedium?.mapperData()
    var appOpen = appOpen?.mapperData()
    var interstitial = inter?.mapperData()
    var rewarded = rewarded?.mapperData()
    var rewardedInterstitial = rewardedInter?.mapperData()

    val overrideConfigFormatAdIdModel: ConfigFormatAdIdModel?

    val countryAdPlaceModelInVn = getField<ConfigFormatAdIdModel>("vn")
    val countryAdPlaceModel = if (countryAdPlaceModelInVn != null && getDeviceLanguageCodes().contains("vi")) {
        countryAdPlaceModelInVn
    } else {
        getField<ConfigFormatAdIdModel>(context.applicationContext.getCountryCode().lowercase())
    }
    overrideConfigFormatAdIdModel = when {
        countryAdPlaceModel != null -> countryAdPlaceModel
        countryTier is CountryTier.Tier1 && tier1 != null -> tier1
        countryTier is CountryTier.Tier2 && tier2 != null -> tier2
        countryTier is CountryTier.Tier3 && tier3 != null -> tier3
        else -> null
    }
    if (overrideConfigFormatAdIdModel != null) {
        overrideConfigFormatAdIdModel.banner?.let { banner = it.mapperData() }
        overrideConfigFormatAdIdModel.nativeSmall?.let { nativeSmall = it.mapperData() }
        overrideConfigFormatAdIdModel.nativeMedium?.let { nativeMedium = it.mapperData() }
        overrideConfigFormatAdIdModel.appOpen?.let { appOpen = it.mapperData() }
        overrideConfigFormatAdIdModel.inter?.let { interstitial = it.mapperData() }
        overrideConfigFormatAdIdModel.rewardedInter?.let { rewardedInterstitial = it.mapperData() }
        overrideConfigFormatAdIdModel.rewarded?.let { rewarded = it.mapperData() }
    }

    return ConfigFormatAdId(
        banner = banner,
        rewarded = rewarded,
        rewardedInterstitial = rewardedInterstitial,
        appOpen = appOpen,
        interstitial = interstitial,
        nativeSmall = nativeSmall,
        nativeMedium = nativeMedium
    )
}