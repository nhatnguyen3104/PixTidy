package com.androidtech.data.model.remoteconfig

import com.androidtech.domain.model.configad.ConfigAdId
import com.androidtech.domain.model.configad.ConfigFormatAdId
import com.androidtech.domain.model.configad.InterAd
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class ConfigAdIdModel(
    @Json(name = "adUnitId")
    val adUnitId: String
) {
    fun toConfigAdId(): ConfigAdId = ConfigAdId(adUnitId = adUnitId)
}

@JsonClass(generateAdapter = true)
data class ConfigFormatAdIdModel(
    @Json(name = "inter")
    val inter: ConfigAdIdModel,

    @Json(name = "app_open")
    val appOpen: ConfigAdIdModel,

    @Json(name = "rewarded")
    val rewarded: ConfigAdIdModel,

    @Json(name = "banner")
    val banner: ConfigAdIdModel,

    @Json(name = "native")
    val native: ConfigAdIdModel,

    @Json(name = "language_native")
    val languageNative: ConfigAdIdModel,

    @Json(name = "splash_banner")
    val splashBanner: ConfigAdIdModel,
) {
    fun transform(): ConfigFormatAdId = ConfigFormatAdId(
        interAd = inter.toConfigAdId(),
        appOpenAd = appOpen.toConfigAdId(),
        rewardedAd = rewarded.toConfigAdId(),
        bannerAd = banner.toConfigAdId(),
        nativeAd = native.toConfigAdId(),
        languageNativeAd = languageNative.toConfigAdId(),
        splashBannerAd = splashBanner.toConfigAdId()
    )
}
