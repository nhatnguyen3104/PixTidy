package com.androidtech.domain.model.configad

sealed interface AdPlace {
    abstract val placeName: String

    abstract val placeScreenLevel: String

    abstract val placeScreenLevelForSubS: String

    abstract val placeScreenLevelInOrder: String?

    abstract val adUnit: AdUnit.AllPrice

    abstract val adIdMedium: AdUnit.MediumPrice

    abstract val adIdHigh: AdUnit.HighPrice

    abstract val isEnable: Boolean

    abstract val isEnableOrganic: Boolean

    abstract val isEnableForSubs: Boolean

    abstract val isCanReuseAds: Boolean

    abstract val adType: AdType

    abstract val isAutoLoadAfterDismiss: Boolean

    abstract val isIgnoreInterval: Boolean

    abstract val isFullscreenAd: Boolean

    fun isNotValidToLoad(isUserCameFromOrganic: Boolean): Boolean {
        return (adUnit.isBlank() && adIdMedium.isBlank() && adIdHigh.isBlank()) || !isEnable || isAdTypeNone() || (isUserCameFromOrganic && !isEnableOrganic)
    }

    fun getValueAdIdHigh(): AdUnit {
        if (adIdHigh.isNotBlank()) return adIdHigh
        if (adIdMedium.isNotBlank()) return adIdMedium
        return adUnit
    }

    fun getValueAdIdMedium(): AdUnit {
        if (adIdMedium.isNotBlank()) return adIdMedium
        return adUnit
    }

    fun getValueAdId(): AdUnit {
        return adUnit
    }

    fun isBannerOrNativePlace() = adType is AdType.Banner || adType is AdType.Native

    fun isBannerType() = adType is AdType.Banner

    fun isNativeType() = adType is AdType.Native

    fun isInterstitialType() = adType is AdType.Interstitial

    fun isRewardedInterstitialType() = adType is AdType.RewardedInterstitial

    fun isRewardedType() = adType is AdType.Rewarded

    fun isAppOpenType() = adType is AdType.AppOpen

    fun isAdTypeNone() = adType is AdType.None
}

class RewardedInterstitialAdPlace(
    override val placeName: String,
    override val placeScreenLevel: String,
    override val placeScreenLevelForSubS: String,
    override val placeScreenLevelInOrder: String?,
    override val adUnit: AdUnit.AllPrice,
    override val adIdMedium: AdUnit.MediumPrice,
    override val adIdHigh: AdUnit.HighPrice,
    override val isEnable: Boolean,
    override val isEnableOrganic: Boolean,
    override val isEnableForSubs: Boolean,
    override val isCanReuseAds: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isFullscreenAd: Boolean = true,
): AdPlace

class InterstitialAdPlace(
    override val placeName: String,
    override val placeScreenLevel: String,
    override val placeScreenLevelForSubS: String,
    override val placeScreenLevelInOrder: String?,
    override val adUnit: AdUnit.AllPrice,
    override val adIdMedium: AdUnit.MediumPrice,
    override val adIdHigh: AdUnit.HighPrice,
    override val isEnable: Boolean,
    override val isEnableOrganic: Boolean,
    override val isEnableForSubs: Boolean,
    override val isCanReuseAds: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isFullscreenAd: Boolean = true
): AdPlace

class NativeAdPlace(
    var nativeTemplateType: NativeTemplateType,

    override val placeName: String,
    override var placeScreenLevel: String,
    override val placeScreenLevelForSubS: String,
    override val placeScreenLevelInOrder: String?,
    override var adUnit: AdUnit.AllPrice,
    override var adIdMedium: AdUnit.MediumPrice,
    override var adIdHigh: AdUnit.HighPrice,
    override val isEnable: Boolean,
    override val isEnableOrganic: Boolean,
    override val isEnableForSubs: Boolean,
    override val isCanReuseAds: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isFullscreenAd: Boolean = false,
): AdPlace {

    fun isSameTemplate(adPlace: AdPlace): Boolean {
        if (adPlace !is NativeAdPlace) {
            return false
        }
        if (nativeTemplateType.isSmall() && adPlace.nativeTemplateType.isSmall()) {
            return true
        }
        if (!nativeTemplateType.isSmall() && !adPlace.nativeTemplateType.isSmall()) {
            return true
        }
        return false
    }
}

class BannerAdPlace(
    var bannerSize: BannerSize,
    val bannerInlineMaxHeight: Int,
    var bannerCollapsiblePosition: String?,
    val bannerCollapsibleIsShowOnlyOnce: Boolean,
    val bannerCollapsibleDelayBeforeShowMillis: Long,
    override val placeName: String,
    override val placeScreenLevel: String,
    override val placeScreenLevelForSubS: String,
    override val placeScreenLevelInOrder: String?,
    override val adUnit: AdUnit.AllPrice,
    override val adIdMedium: AdUnit.MediumPrice,
    override val adIdHigh: AdUnit.HighPrice,
    override var isEnable: Boolean,
    override val isEnableOrganic: Boolean,
    override val isEnableForSubs: Boolean,
    override val isCanReuseAds: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isFullscreenAd: Boolean = false,
): AdPlace{
    fun getKeyCollapsible() = "$bannerCollapsiblePosition"
}

class AppOpenAdPlace(
    val appOpenDelayBeforeShowMillis: Long,
    override val placeName: String,
    override val placeScreenLevel: String,
    override val placeScreenLevelForSubS: String,
    override val placeScreenLevelInOrder: String?,
    override val adUnit: AdUnit.AllPrice,
    override val adIdMedium: AdUnit.MediumPrice,
    override val adIdHigh: AdUnit.HighPrice,
    override val isEnable: Boolean,
    override val isEnableOrganic: Boolean,
    override val isEnableForSubs: Boolean,
    override val isCanReuseAds: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isFullscreenAd: Boolean = false,
): AdPlace

class RewardedAdPlace(
    override val placeName: String,
    override val placeScreenLevel: String,
    override val placeScreenLevelForSubS: String,
    override val placeScreenLevelInOrder: String?,
    override val adUnit: AdUnit.AllPrice,
    override val adIdMedium: AdUnit.MediumPrice,
    override val adIdHigh: AdUnit.HighPrice,
    override val isEnable: Boolean,
    override val isEnableOrganic: Boolean,
    override val isEnableForSubs: Boolean,
    override val isCanReuseAds: Boolean,
    override val adType: AdType,
    override val isAutoLoadAfterDismiss: Boolean,
    override val isIgnoreInterval: Boolean,
    override val isFullscreenAd: Boolean = true,
): AdPlace

class NoneAdPlace(
    override val placeName: String = "",
    override val placeScreenLevel: String = "",
    override val placeScreenLevelForSubS: String = "",
    override val placeScreenLevelInOrder: String? = "",
    override val adUnit: AdUnit.AllPrice = AdUnit.AllPrice(""),
    override val adIdMedium: AdUnit.MediumPrice = AdUnit.MediumPrice(""),
    override val adIdHigh: AdUnit.HighPrice = AdUnit.HighPrice(""),
    override val isEnable: Boolean = false,
    override val isEnableOrganic: Boolean = false,
    override val isEnableForSubs: Boolean = false,
    override val isCanReuseAds: Boolean = false,
    override val adType: AdType = AdType.None,
    override val isAutoLoadAfterDismiss: Boolean = false,
    override val isIgnoreInterval: Boolean = false,
    override val isFullscreenAd: Boolean = false,
): AdPlace