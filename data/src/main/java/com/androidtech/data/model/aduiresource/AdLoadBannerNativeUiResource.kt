package com.androidtech.data.model.aduiresource

import android.view.ViewGroup
import com.androidtech.data.model.aduiresource.AdLoadBannerNativeUiResource.AdFailed
import com.androidtech.data.model.aduiresource.AdLoadBannerNativeUiResource.AdImpression
import com.androidtech.data.model.aduiresource.AdLoadBannerNativeUiResource.BannerAdLoaded
import com.androidtech.data.model.aduiresource.AdLoadBannerNativeUiResource.Loading
import com.androidtech.data.model.aduiresource.AdLoadBannerNativeUiResource.NativeAdLoaded
import com.androidtech.domain.model.configad.BannerAdPlace

import com.androidtech.domain.model.configad.NativeAdPlace
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

sealed class AdLoadBannerNativeUiResource {

    sealed class Loading : AdLoadBannerNativeUiResource() {
        open class LoadingBanner(
            val bannerAdPlace: BannerAdPlace,
            val subPlaceLevel: Int,
        ) : Loading()

        class LoadingNative(
            val nativeAdPlace: NativeAdPlace,
            val subPlaceLevel: Int,
        ) : Loading()
    }

    class AdFailed(
        val adPlaceName: String,
        val subPlaceLevel: Int,
    ) : AdLoadBannerNativeUiResource()

    class AdImpression(
        val placeName: String,
        val subPlaceLevel: Int,
    ) : AdLoadBannerNativeUiResource()

    class BannerAdLoaded(
        val bannerAd: ViewGroup,
        val bannerAdPlace: BannerAdPlace,
        val subPlaceLevel: Int,
        val isCollapsible: Boolean = false,
    ) : AdLoadBannerNativeUiResource()

    class NativeAdLoaded(
        val nativeAd: NativeAd,
        val nativeAdPlace: NativeAdPlace,
        val subPlaceLevel: Int,
    ) : AdLoadBannerNativeUiResource()

    fun isLoaded() = this is BannerAdLoaded || this is NativeAdLoaded
}

fun AdLoadBannerNativeUiResource.getAdPlaceName(): String = when (this) {
    is Loading.LoadingBanner -> bannerAdPlace.placeName
    is Loading.LoadingNative -> nativeAdPlace.placeName
    is AdFailed -> adPlaceName
    is AdImpression -> placeName
    is BannerAdLoaded -> bannerAdPlace.placeName
    is NativeAdLoaded -> nativeAdPlace.placeName
}