package com.androidtech.domain.model.configad

data class ConfigAdId(
    val adUnitId: String
)

data class ConfigFormatAdId(
    val interAd: ConfigAdId? = null,
    val rewardedAd: ConfigAdId? = null,
    val appOpenAd: ConfigAdId? = null,
    val bannerAd: ConfigAdId? = null,
    val nativeAd: ConfigAdId? = null,
    val languageNativeAd: ConfigAdId? = null,
    val splashBannerAd: ConfigAdId? = null
) {
    fun getConfigAdIdByType(adType: AdType): ConfigAdId {
        return ConfigAdId(
            when (adType) {
                AdType.Interstitial -> interAd?.adUnitId ?: ""
                AdType.Rewarded -> rewardedAd?.adUnitId ?: ""
                AdType.AppOpen -> appOpenAd?.adUnitId ?: ""
                AdType.Banner -> bannerAd?.adUnitId ?: ""
                AdType.Native -> nativeAd?.adUnitId ?: ""
                AdType.LanguageNative -> languageNativeAd?.adUnitId ?: ""
                AdType.SplashBanner -> splashBannerAd?.adUnitId ?: ""
            }
        )
    }
}
