package com.ads.monetization.advance

import android.app.Activity
import android.util.Log
import com.ads.monetization.AdManager
import com.ads.monetization.config.AdConfig
import com.ads.monetization.config.AdType

class SmartAdManager(
    private val adManager: AdManager,
    private val frequencyManager: AdFrequencyManager
) {

    fun showInterstitialAdIfAllowed(
        activity: Activity,
        onAdShown: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ) {
        if (!frequencyManager.canShowAd(AdType.INTERSTITIAL)) {
            Log.d(AdConfig.TAG, "Interstitial ad blocked by frequency cap")
            onAdFailed("Frequency limit reached")
            return
        }

        adManager.showAdIfAvailable(
            activity = activity,
            adType = AdType.INTERSTITIAL,
            onAdShown = {
                frequencyManager.recordAdShown(AdType.INTERSTITIAL)
                onAdShown()
            },
            onAdFailed = onAdFailed
        )
    }
}