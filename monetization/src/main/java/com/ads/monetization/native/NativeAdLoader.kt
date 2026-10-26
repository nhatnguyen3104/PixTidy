package com.ads.monetization.native

import android.content.Context
import android.util.Log
import com.ads.monetization.config.AdConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAd.OnNativeAdLoadedListener
import com.google.android.gms.ads.nativead.NativeAdOptions

class NativeAdLoader(
    private val adUnitId: String
) {

    private var nativeAd: NativeAd? = null
    private var isLoadingAd = false

    fun loadAd(
        context: Context,
        onAdLoaded: (NativeAd) -> Unit = {},
        onAdFailedToLoad: (LoadAdError) -> Unit = {}
    ) {
        if (isLoadingAd) {
            Log.d(AdConfig.TAG, "Ad đang load")
            return
        }

        isLoadingAd = true

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd(NativeAd.OnNativeAdLoadedListener { ad ->
                Log.d(AdConfig.TAG, "Native Ad loaded thành công")

                // Nếu có ad cũ, destroy nó trước
                if (nativeAd != null && nativeAd != ad) {
                    nativeAd?.destroy()
                }

                nativeAd = ad
                isLoadingAd = false
                onAdLoaded(ad)
            })
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(AdConfig.TAG, "Native Ad failed: ${error.message}")
                    isLoadingAd = false
                    onAdFailedToLoad(error)
                }

                override fun onAdClicked() {
                    Log.d(AdConfig.TAG, "Native Ad clicked")
                }

                override fun onAdOpened() {
                    Log.d(AdConfig.TAG, "Native Ad opened")
                }

                override fun onAdClosed() {
                    Log.d(AdConfig.TAG, "Native Ad closed")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    fun getNativeAd(): NativeAd? = nativeAd

    fun isAdAvailable(): Boolean = nativeAd != null

    fun destroy() {
        nativeAd?.destroy()
        nativeAd = null
        isLoadingAd = false
    }
}