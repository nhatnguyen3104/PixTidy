package com.ads.monetization

import android.app.Activity
import com.ads.monetization.config.AdType
import com.ads.monetization.config.ConsentFromUiResource
import kotlinx.coroutines.flow.SharedFlow

interface AdManager {

    val requestConsentFlow: SharedFlow<ConsentFromUiResource>

    fun initializeMobileAdsSdk()

    fun preloadFullScreenAd(
        activity: Activity,
        placeName: String,
        isNeedNotifyEvent: Boolean = false
    )

    fun preloadAds(activity: Activity)

    fun preloadAd(
        activity: Activity,
        adType: AdType
    )

    fun showAdIfAvailable(
        activity: Activity,
        adType: AdType,
        onAdShown: () -> Unit = {},
        onAdFailed: (String) -> Unit = {},
        onAdClosed: () -> Unit = {}
    )

    fun isAdLoaded(adType: AdType): Boolean

    /**
     * Xóa cache quảng cáo
     */
    fun clearAd(adType: AdType)

    /**
     * Xóa tất cả quảng cáo
     */
    fun clearAllAds()
}