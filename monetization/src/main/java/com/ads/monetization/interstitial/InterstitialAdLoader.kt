package com.ads.monetization.interstitial

import android.app.Activity
import android.util.Log
import com.ads.monetization.config.AdConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdLoader(
    private val adUnitId: String
) {
    private var mInterstitialAd: InterstitialAd? = null
    private var isLoadingAd = false

    fun loadAd(
        activity: Activity,
        onAdLoaded: () -> Unit = {},
        onAdFailedToLoad: (LoadAdError) -> Unit = {}
    ) {
        if (isLoadingAd || mInterstitialAd != null) {
            Log.d(AdConfig.TAG, "Ad đang load hoặc đã có sẵn")
            return
        }
        isLoadingAd = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            activity,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitalAd: InterstitialAd) {
                    super.onAdLoaded(interstitalAd)
                    Log.d(AdConfig.TAG, "Interstitial Ad loaded")
                    mInterstitialAd = interstitalAd
                    isLoadingAd = false
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    Log.d(AdConfig.TAG, "Interstitial Ad failed to load")
                    isLoadingAd = false
                    onAdFailedToLoad(loadAdError)
                }
            }
        )
    }

    fun showAdIfAvailable(
        activity: Activity,
        onAdShown: () -> Unit = {},
        onAdFailed: (String) -> Unit = {},
        onAdDismissed: () -> Unit = {}
    ) {
        if (mInterstitialAd == null) {
            Log.d(AdConfig.TAG, "Interstitial Ad chưa sẵn sàng")
            onAdFailed("Ad chưa được load")
            loadAd(activity)
            return
        }

        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(AdConfig.TAG, "Interstitial Ad dismissed")
                mInterstitialAd = null
                onAdDismissed()
                loadAd(activity) // Preload ad tiếp theo
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(AdConfig.TAG, "Interstitial Ad failed to show: ${adError.message}")
                mInterstitialAd = null
                onAdFailed(adError.message)
                loadAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(AdConfig.TAG, "Interstitial Ad showed")
                onAdShown()
            }

            override fun onAdClicked() {
                Log.d(AdConfig.TAG, "Interstitial Ad clicked")
            }

            override fun onAdImpression() {
                Log.d(AdConfig.TAG, "Interstitial Ad impression recorded")
            }
        }
        mInterstitialAd?.show(activity)
    }

    fun isAdAvailable(): Boolean {
        return mInterstitialAd != null
    }

    fun clear() {
        mInterstitialAd = null
        isLoadingAd = false
    }
}