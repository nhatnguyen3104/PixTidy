package com.androidtech.data.model.adplace

import android.content.Context
import com.androidtech.data.remoteconfig.CountryTier
import com.androidtech.data.utils.getCountryCode
import com.androidtech.data.utils.getDeviceLanguageCodes
import com.androidtech.data.utils.getField
import com.androidtech.domain.model.configad.AdPlace
import com.androidtech.domain.model.configad.AdType
import com.androidtech.domain.model.configad.AdUnit
import com.androidtech.domain.model.configad.AppOpenAdPlace
import com.androidtech.domain.model.configad.BannerAdPlace
import com.androidtech.domain.model.configad.BannerSize
import com.androidtech.domain.model.configad.ConfigFormatAdId
import com.androidtech.domain.model.configad.InterstitialAdPlace
import com.androidtech.domain.model.configad.NativeAdPlace
import com.androidtech.domain.model.configad.NativeTemplateType
import com.androidtech.domain.model.configad.NoneAdPlace
import com.androidtech.domain.model.configad.RewardedAdPlace
import com.androidtech.domain.model.configad.RewardedInterstitialAdPlace
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class AdPlaceModel(
    @param:Json(name = "name")
    val name: String?,

    @param:Json(name = "ad_id_v1")
    val adId: String?,

    @param:Json(name = "ad_id_v2")
    val adIdV2: String?,

    @param:Json(name = "ad_id_v3")
    val adIdV3: String?,

    @param:Json(name = "ad_type")
    val adType: String?,

    @param:Json(name = "is_enable")
    val isEnable: Boolean?,

    @param:Json(name = "is_auto_load_after_dismiss")
    val isAutoLoadAfterDismiss: Boolean?,

    // For AppOpenApp
    @param:Json(name = "app_open_delay_before_show_millis")
    val appOpenDelayBeforeShowMillis: Long?,

    // For Interstitial
    @param:Json(name = "native_full_after_inter_name")
    val nativeFullAfterInterName: String?,

    // For BannerType (anchored or inline)
    @param:Json(name = "banner_type")
    val bannerType: String?,

    @param:Json(name = "banner_inline_max_height")
    val bannerInlineMaxHeight: Int?,

    @param:Json(name = "banner_collapsible_position")
    val bannerCollapsiblePosition: String?,

    @param:Json(name = "banner_collapsible_is_show_only_once")
    val bannerCollapsibleIsShowOnlyOnce: Boolean?,

    @param:Json(name = "banner_collapsible_delay_before_show_millis")
    val bannerCollapsibleDelayBeforeShowMillis: Long?,

    // For NativeAd
    @param:Json(name = "native_template_type")
    val nativeTemplateType: String?,

    @param:Json(name = "t1")
    val tier1: AdPlaceModel?,

    @param:Json(name = "t2")
    val tier2: AdPlaceModel?,

    @param:Json(name = "t3")
    val tier3: AdPlaceModel?,
)

fun AdPlaceModel.mapperModel(
    context: Context,
    countryTier: CountryTier,
    configFormatAdId: ConfigFormatAdId
): AdPlace {
    val overrideAdPlaceModel: AdPlaceModel?

    val countryAdPlaceModelInVn = getField<AdPlaceModel>("vn")
    val countryAdPlaceModel = if (countryAdPlaceModelInVn != null && (getDeviceLanguageCodes().contains("vi") || context.applicationContext.getCountryCode().lowercase() == "vn")) {
        countryAdPlaceModelInVn
    } else {
        getField<AdPlaceModel>(context.applicationContext.getCountryCode().lowercase())
    }

    overrideAdPlaceModel = when {
        countryAdPlaceModel != null -> countryAdPlaceModel
        countryTier is CountryTier.Tier1 && tier1 != null -> tier1
        countryTier is CountryTier.Tier2 && tier2 != null -> tier2
        countryTier is CountryTier.Tier3 && tier3 != null -> tier3
        else -> null
    }

    // 2. Lấy thông tin cơ bản
    var nativeTemplateType = NativeTemplateType.getTypeBy(nativeTemplateType ?: "")
    overrideAdPlaceModel?.nativeTemplateType?.let {
        nativeTemplateType = NativeTemplateType.getTypeBy(it)
    }

    val placeName = name ?: ""
    var adType = AdType.getAdTypeBy(adType ?: "")
    var isEnable = isEnable ?: true
    var isAutoLoadAfterDismiss = isAutoLoadAfterDismiss ?: true

    var adId: AdUnit.AllPrice = AdUnit.AllPrice("")
    var adIdMedium: AdUnit.MediumPrice = AdUnit.MediumPrice("")
    var adIdHigh: AdUnit.HighPrice = AdUnit.HighPrice("")

    // 3. Lấy ID mặc định từ configFormatAdId
    val configAdId = configFormatAdId.getConfigAdIdByAdType(adType, nativeTemplateType)
    if (configAdId != null) {
        if (configAdId.allPrice.isNotBlank()) adId = AdUnit.AllPrice(configAdId.allPrice)
        if (configAdId.mediumPrice.isNotBlank()) adIdMedium = AdUnit.MediumPrice(configAdId.mediumPrice)
        if (configAdId.highPrice.isNotBlank()) adIdHigh = AdUnit.HighPrice(configAdId.highPrice)
    }

    // 4. Đè bằng ID trực tiếp từ cấu hình gốc nếu có
    if (!this.adId.isNullOrBlank()) adId = AdUnit.AllPrice(this.adId)
    if (!adIdV2.isNullOrBlank()) adIdMedium = AdUnit.MediumPrice(adIdV2)
    if (!adIdV3.isNullOrBlank()) adIdHigh = AdUnit.HighPrice(adIdV3)

    // 5. Đè bằng cấu hình của Quốc gia / Tier cụ thể (nếu có override)
    if (overrideAdPlaceModel != null) {
        overrideAdPlaceModel.adType?.let {
            val newAdType = AdType.getAdTypeBy(it)
            if (newAdType != adType) {
                adId = AdUnit.AllPriceEmpty
                adIdMedium = AdUnit.MediumPriceEmpty
                adIdHigh = AdUnit.HighPriceEmpty
            }
            adType = newAdType
        }

        val configAdIdOverride = configFormatAdId.getConfigAdIdByAdType(adType, nativeTemplateType)
        if (configAdIdOverride != null) {
            if (configAdIdOverride.allPrice.isNotBlank()) adId = AdUnit.AllPrice(configAdIdOverride.allPrice)
            if (configAdIdOverride.mediumPrice.isNotBlank()) adIdMedium = AdUnit.MediumPrice(configAdIdOverride.mediumPrice)
            if (configAdIdOverride.highPrice.isNotBlank()) adIdHigh = AdUnit.HighPrice(configAdIdOverride.highPrice)
        }

        if (!overrideAdPlaceModel.adId.isNullOrBlank()) adId = AdUnit.AllPrice(overrideAdPlaceModel.adId)
        if (!overrideAdPlaceModel.adIdV2.isNullOrBlank()) adIdMedium = AdUnit.MediumPrice(overrideAdPlaceModel.adIdV2)
        if (!overrideAdPlaceModel.adIdV3.isNullOrBlank()) adIdHigh = AdUnit.HighPrice(overrideAdPlaceModel.adIdV3)

        overrideAdPlaceModel.isEnable?.let { isEnable = it }
        overrideAdPlaceModel.isAutoLoadAfterDismiss?.let { isAutoLoadAfterDismiss = it }
    }

    val isFullscreenAd = adType is AdType.Interstitial ||
            adType is AdType.RewardedInterstitial ||
            adType is AdType.AppOpen ||
            adType is AdType.Rewarded

    // Các giá trị mặc định cho interface AdPlace
    val placeScreenLevel = ""
    val placeScreenLevelForSubS = ""
    val placeScreenLevelInOrder: String? = null
    val isEnableOrganic = true
    val isEnableForSubs = (adType is AdType.Banner || adType is AdType.Native)
    val isCanReuseAds = false
    val isIgnoreInterval = false

    // 6. Trả về đúng loại AdPlace tương ứng
    return when (adType) {
        AdType.Banner -> BannerAdPlace(
            bannerSize = BannerSize.getSizeBy(bannerType ?: ""),
            bannerInlineMaxHeight = bannerInlineMaxHeight ?: 0,
            bannerCollapsiblePosition = bannerCollapsiblePosition,
            bannerCollapsibleIsShowOnlyOnce = bannerCollapsibleIsShowOnlyOnce ?: true,
            bannerCollapsibleDelayBeforeShowMillis = bannerCollapsibleDelayBeforeShowMillis ?: 0L,
            placeName = placeName,
            placeScreenLevel = placeScreenLevel,
            placeScreenLevelForSubS = placeScreenLevelForSubS,
            placeScreenLevelInOrder = placeScreenLevelInOrder,
            adUnit = adId,
            adIdMedium = adIdMedium,
            adIdHigh = adIdHigh,
            isEnable = isEnable,
            isEnableOrganic = isEnableOrganic,
            isEnableForSubs = isEnableForSubs,
            isCanReuseAds = isCanReuseAds,
            adType = adType,
            isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
            isIgnoreInterval = isIgnoreInterval,
            isFullscreenAd = isFullscreenAd,
        )

        AdType.Native -> NativeAdPlace(
            nativeTemplateType = nativeTemplateType,
            placeName = placeName,
            placeScreenLevel = placeScreenLevel,
            placeScreenLevelForSubS = placeScreenLevelForSubS,
            placeScreenLevelInOrder = placeScreenLevelInOrder,
            adUnit = adId,
            adIdMedium = adIdMedium,
            adIdHigh = adIdHigh,
            isEnable = isEnable,
            isEnableOrganic = isEnableOrganic,
            isEnableForSubs = isEnableForSubs,
            isCanReuseAds = isCanReuseAds,
            adType = adType,
            isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
            isIgnoreInterval = isIgnoreInterval,
            isFullscreenAd = isFullscreenAd,
        )

        AdType.Interstitial -> InterstitialAdPlace(
            placeName = placeName,
            placeScreenLevel = placeScreenLevel,
            placeScreenLevelForSubS = placeScreenLevelForSubS,
            placeScreenLevelInOrder = placeScreenLevelInOrder,
            adUnit = adId,
            adIdMedium = adIdMedium,
            adIdHigh = adIdHigh,
            isEnable = isEnable,
            isEnableOrganic = isEnableOrganic,
            isEnableForSubs = isEnableForSubs,
            isCanReuseAds = isCanReuseAds,
            adType = adType,
            isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
            isIgnoreInterval = isIgnoreInterval,
            isFullscreenAd = isFullscreenAd
        )

        AdType.AppOpen -> AppOpenAdPlace(
            appOpenDelayBeforeShowMillis = appOpenDelayBeforeShowMillis ?: 0L,
            placeName = placeName,
            placeScreenLevel = placeScreenLevel,
            placeScreenLevelForSubS = placeScreenLevelForSubS,
            placeScreenLevelInOrder = placeScreenLevelInOrder,
            adUnit = adId,
            adIdMedium = adIdMedium,
            adIdHigh = adIdHigh,
            isEnable = isEnable,
            isEnableOrganic = isEnableOrganic,
            isEnableForSubs = isEnableForSubs,
            isCanReuseAds = isCanReuseAds,
            adType = adType,
            isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
            isIgnoreInterval = isIgnoreInterval,
            isFullscreenAd = isFullscreenAd
        )

        AdType.Rewarded -> RewardedAdPlace(
            placeName = placeName,
            placeScreenLevel = placeScreenLevel,
            placeScreenLevelForSubS = placeScreenLevelForSubS,
            placeScreenLevelInOrder = placeScreenLevelInOrder,
            adUnit = adId,
            adIdMedium = adIdMedium,
            adIdHigh = adIdHigh,
            isEnable = isEnable,
            isEnableOrganic = isEnableOrganic,
            isEnableForSubs = isEnableForSubs,
            isCanReuseAds = isCanReuseAds,
            adType = adType,
            isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
            isIgnoreInterval = isIgnoreInterval,
            isFullscreenAd = isFullscreenAd
        )

        AdType.RewardedInterstitial -> RewardedInterstitialAdPlace(
            placeName = placeName,
            placeScreenLevel = placeScreenLevel,
            placeScreenLevelForSubS = placeScreenLevelForSubS,
            placeScreenLevelInOrder = placeScreenLevelInOrder,
            adUnit = adId,
            adIdMedium = adIdMedium,
            adIdHigh = adIdHigh,
            isEnable = isEnable,
            isEnableOrganic = isEnableOrganic,
            isEnableForSubs = isEnableForSubs,
            isCanReuseAds = isCanReuseAds,
            adType = adType,
            isAutoLoadAfterDismiss = isAutoLoadAfterDismiss,
            isIgnoreInterval = isIgnoreInterval,
            isFullscreenAd = isFullscreenAd
        )

        else -> NoneAdPlace()
    }
}