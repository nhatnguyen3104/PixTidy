package com.ads.monetization.rewarded

import android.app.Activity
import android.util.Log
import com.ads.monetization.config.AdConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class RewardedAdLoader(
    private val adUnitId: String
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoadingAd = false

    fun loadAd(
        activity: Activity,
        onAdLoaded: () -> Unit = {},
        onAdFailedToLoad: (LoadAdError) -> Unit = {}
    ) {
        if (isLoadingAd || rewardedAd != null) {
            Log.d(AdConfig.TAG, "Ad đang load hoặc đã có sẵn")
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()

        RewardedAd.load(
            activity,
            adUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(AdConfig.TAG, "Rewarded Ad loaded thành công")
                    rewardedAd = ad
                    isLoadingAd = false
                    onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(AdConfig.TAG, "Rewarded Ad failed: ${loadAdError.message}")
                    rewardedAd = null
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
        onAdDismissed: () -> Unit = {},
        onUserEarnedReward: (rewardType: String, rewardAmount: Int) -> Unit = { _, _ -> }
    ) {
        if (rewardedAd == null) {
            Log.d(AdConfig.TAG, "Rewarded Ad chưa sẵn sàng")
            onAdFailed("Ad chưa được load")
            loadAd(activity)
            return
        }

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(AdConfig.TAG, "Rewarded Ad dismissed")
                rewardedAd = null
                onAdDismissed()
                loadAd(activity) // Preload ad tiếp theo
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(AdConfig.TAG, "Rewarded Ad failed to show: ${adError.message}")
                rewardedAd = null
                onAdFailed(adError.message)
                loadAd(activity)
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(AdConfig.TAG, "Rewarded Ad showed")
                onAdShown()
            }

            override fun onAdClicked() {
                Log.d(AdConfig.TAG, "Rewarded Ad clicked")
            }

            override fun onAdImpression() {
                Log.d(AdConfig.TAG, "Rewarded Ad impression recorded")
            }
        }

        rewardedAd?.show(activity) { rewardItem ->
            val rewardType = rewardItem.type
            val rewardAmount = rewardItem.amount
            Log.d(AdConfig.TAG, "User earned reward: $rewardAmount $rewardType")
            onUserEarnedReward(rewardType, rewardAmount)
        }
    }

    fun isAdAvailable(): Boolean {
        return rewardedAd != null
    }

    fun clear() {
        rewardedAd = null
        isLoadingAd = false
    }
}