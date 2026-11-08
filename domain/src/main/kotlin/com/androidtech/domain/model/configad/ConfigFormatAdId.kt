package com.androidtech.domain.model.configad

data class ConfigAdId(
    val adUnitId: String,
    val allPrice: String,
    val mediumPrice: String,
    val highPrice: String,
)

data class ConfigFormatAdId(
    val interstitial: ConfigAdId? = null,
    val rewarded: ConfigAdId? = null,
    val rewardedInterstitial: ConfigAdId? = null,
    val appOpen: ConfigAdId? = null,
    val banner: ConfigAdId? = null,
    val nativeSmall: ConfigAdId? = null,
    val nativeMedium: ConfigAdId? = null
) {
    companion object {
        val default = ConfigFormatAdId()
    }

    fun getConfigAdIdByAdType(adType: AdType, nativeTemplateType: NativeTemplateType): ConfigAdId? {
        return when (adType) {
            AdType.AppOpen -> appOpen
            AdType.Banner -> banner
            AdType.Interstitial -> interstitial
            AdType.Native -> {
                when {
                    nativeTemplateType.isSmall() -> nativeSmall
                    else -> nativeMedium
                }
            }
            AdType.Rewarded -> rewarded
            AdType.RewardedInterstitial -> rewardedInterstitial
            else -> null
        }
    }
}
