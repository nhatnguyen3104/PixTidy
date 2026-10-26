package com.ads.monetization.banner

import android.app.Activity
import android.util.Log
import com.ads.monetization.config.AdConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class BannerAdLoader(
    private val adUnitId: String
) {

    private var adView: AdView? = null

    fun createAdView(
        activity: Activity,
        adSize: AdSize = AdSize.BANNER,
        onAdLoaded: () -> Unit = {},
        onAdFailedToLoad: (LoadAdError) -> Unit = {}
    ): AdView {
        return AdView(activity).apply {
            setAdSize(adSize)
            this.adUnitId = this@BannerAdLoader.adUnitId

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    Log.d(AdConfig.TAG, "Banner Ad loaded thành công")
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(AdConfig.TAG, "Banner Ad failed: ${error.message}")
                    onAdFailedToLoad(error)
                }

                override fun onAdClicked() {
                    Log.d(AdConfig.TAG, "Banner Ad clicked")
                }

                override fun onAdOpened() {
                    Log.d(AdConfig.TAG, "Banner Ad opened")
                }

                override fun onAdClosed() {
                    Log.d(AdConfig.TAG, "Banner Ad closed")
                }
            }
            adView = this
            loadAd(AdRequest.Builder().build())
        }
    }

    fun destroy() {
        adView?.destroy()
        adView = null
    }

    fun pause() {
        adView?.pause()
    }

    fun resume() {
        adView?.resume()
    }
}