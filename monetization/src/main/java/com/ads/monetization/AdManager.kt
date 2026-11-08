package com.ads.monetization

import android.app.Activity
import com.androidtech.data.model.aduiresource.AdFullScreenUiResource
import com.androidtech.data.model.aduiresource.AdLoadBannerNativeUiResource
import com.androidtech.domain.model.configad.AdUnit
import com.androidtech.domain.model.configad.ConfigPreventAdClick
import com.androidtech.domain.model.configad.AdPlace
import com.ads.monetization.adholder.BannerAdHolder
import com.ads.monetization.adholder.NativeAdHolder
import com.ads.monetization.config.ConsentFromUiResource
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdSourceResponseInfo
import kotlinx.coroutines.flow.SharedFlow

interface AdManager {

    val requestConsentFlow: SharedFlow<ConsentFromUiResource>

    val adBannerNativeFlow: SharedFlow<AdLoadBannerNativeUiResource>

    val adFullScreenFlow: SharedFlow<AdFullScreenUiResource>

    fun initializeMobileAdsSdk()

    fun getConfigCountriesDisableAds(): List<String>

    fun getConfigPreventAdClick(): ConfigPreventAdClick

    fun startDisableAdCountDownTimer()

    fun isNotAbleToVisibleAdsToUser(placeName: String): Boolean

    fun isCannotShowInterAd(adPlace: AdPlace): Boolean

    fun getAdUnit(adPlace: AdPlace): AdUnit

    fun getAdPlaceBy(adPlaceName: String): AdPlace

    fun getAdRequest(adId: String): AdRequest

    fun getUniqueAdSourceName(loadedAdapterResponseInfo: AdSourceResponseInfo?): String

    fun preloadFullScreenAd(
        activity: Activity,
        placeName: String
    )

    fun preloadBannerNativeAd(
        placeName: String,
        subPlaceLevel: Int = 0,
        isCanShowBannerCollapsibleIfNeed: Boolean = true,
        isPreloadForTheSameActivity: Boolean = true
    )

    fun showFullScreenAd(activity: Activity, placeName: String)

    fun showBannerNativeAd(
        placeName: String,
        subPlaceLevel: Int = 0,
        isCanShowBannerCollapsibleIfNeed: Boolean = true,
    )

    fun releaseBannerNative(
        placeName: String,
        subPlaceLevel: Int = 0
    )

    fun getBannerAdHolder(
        placeName: String,
        subPlaceLevel: Int = 0
    ): BannerAdHolder?

    fun getNativeAdHolder(
        placeName: String,
        subPlaceLevel: Int = 0
    ): NativeAdHolder?
}