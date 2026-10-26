package com.ads.monetization.openad

import android.app.Activity
import android.util.Log
import com.ads.monetization.config.AdConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

class AppOpenAdLoader(
    private val adUnitId: String
) {

    companion object {
        private const val TAG = "AppOpenAdLoader"
        private const val AD_TIMEOUT_MS = 4 * 60 * 60 * 1000L // 4 hours
    }

    private var mAppOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var loadTime = 0L

    fun loadAd(
        activity: Activity,
        onAdLoaded: () -> Unit = {},
        onAdLoadFailed: (LoadAdError) -> Unit = {}
    ) {
        if (isLoadingAd || isAdAvailable()) {
            return
        }
        isLoadingAd = true
        val request = AdRequest.Builder().build()

        AppOpenAd.load(
            activity,
            adUnitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    super.onAdLoaded(appOpenAd)
                    Log.d(AdConfig.TAG, "App Open Ad loaded")
                    mAppOpenAd = appOpenAd
                    isLoadingAd = false
                    loadTime = System.currentTimeMillis()
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    Log.d(AdConfig.TAG, "App Open Ad load failed: ${loadAdError.message}")
                    isLoadingAd = false
                    onAdLoadFailed(loadAdError)
                }
            }
        )
    }

    fun showIfAvailable(
        activity: Activity,
        onAdShown: () -> Unit = {},
        onAdFailed: (String) -> Unit = {},
        onAdDismissed: () -> Unit = {}
    ) {
        if (!isAdAvailable()) {
            Log.d(AdConfig.TAG, "App Open Ad not available")
            onAdFailed("Ad chưa được load hoặc đã hết hạn")
            loadAd(activity)
            return
        }

        mAppOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App Open Ad dismissed")
                mAppOpenAd = null
                onAdDismissed()
                loadAd(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "App Open Ad failed to show: ${adError.message}")
                mAppOpenAd = null
                onAdFailed(adError.message)
                loadAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App Open Ad showed")
                onAdShown()
            }
        }
    }

    fun isAdAvailable(): Boolean {
        return mAppOpenAd != null && wasLoadTimeLessThanNHoursAgo()
    }

    private fun wasLoadTimeLessThanNHoursAgo(): Boolean {
        val dateDifference = System.currentTimeMillis() - loadTime
        return dateDifference < AD_TIMEOUT_MS
    }

    fun clear() {
        mAppOpenAd = null
        isLoadingAd = false
        loadTime = 0
    }
}