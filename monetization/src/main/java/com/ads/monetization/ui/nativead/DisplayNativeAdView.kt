package com.ads.monetization.ui.nativead

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.androidtech.domain.model.configad.AdPlace
import com.androidtech.domain.model.configad.NativeAdPlace
import com.androidtech.domain.model.configad.NativeTemplateType
import com.ads.monetization.adholder.NativeAdHolder
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd

/**
 * Template dùng khi `adPlace` không phải [NativeAdPlace] hoặc config trả về giá trị lạ.
 * [NativeTemplateType.Medium] là lựa chọn an toàn: chiều cao cố định, không nuốt màn hình
 * như [NativeTemplateType.Full].
 */
private val DEFAULT_NATIVE_TEMPLATE = NativeTemplateType.Medium

@Composable
fun DisplayNativeAdView(
    ad: NativeAd,
    templateType: NativeTemplateType,
    modifier: Modifier = Modifier,
) {
    when {
        templateType.isFull() -> FullNativeAdView(ad, modifier)
        templateType.isSmall() -> SmallNativeAdView(ad, modifier)
        else -> MediumNativeAdView(ad, modifier)
    }
}

@Composable
fun DisplayNativeAdView(
    adHolder: NativeAdHolder?,
    modifier: Modifier = Modifier,
) {
    val nativeAd = adHolder?.nativeAd ?: return
    DisplayNativeAdView(
        ad = nativeAd,
        adPlace = adHolder.adPlace,
        modifier = modifier
    )
}

@Composable
fun DisplayNativeAdView(
    ad: NativeAd,
    adPlace: AdPlace?,
    modifier: Modifier = Modifier,
) {
    val templateType = (adPlace as? NativeAdPlace)?.nativeTemplateType ?: DEFAULT_NATIVE_TEMPLATE
    DisplayNativeAdView(
        ad = ad,
        templateType = templateType,
        modifier = modifier
    )
}