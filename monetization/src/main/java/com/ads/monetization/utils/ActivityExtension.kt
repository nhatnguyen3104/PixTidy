package com.ads.monetization.utils

import android.app.Activity
import androidx.annotation.StringRes
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

fun Activity.loadFullScreenAd(
    @StringRes adUnitId: Int,
    adRequest: AdRequest = AdRequest.Builder().build(),
    callBack: InterstitialAdLoadCallback? = null
) {
    InterstitialAd.load(
        this,
        getString(adUnitId),
        adRequest,
        callBack ?: object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                super.onAdLoaded(interstitialAd)
                interstitialAd.show(this@loadFullScreenAd)
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
            }
        }
    )
}