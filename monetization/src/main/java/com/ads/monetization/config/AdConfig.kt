package com.ads.monetization.config

data class AdConfig(
    val appOpenAdId: String = "",
    val bannerAdId: String = "",
    val nativeAdId: String = "",
    val rewardedAdId: String = "",
    val interstitialAdId: String = "",
    val enableTestAds: Boolean = false
) {
    companion object {
        const val TAG = "GeneralApp"
        // Test Ad IDs từ Google
        const val TEST_APP_OPEN_AD_ID = "ca-app-pub-3940256099942544/9257395921"
        const val TEST_BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
        const val TEST_NATIVE_AD_ID = "ca-app-pub-3940256099942544/2247696110"
        const val TEST_REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"
        const val TEST_INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"

        fun testConfig() = AdConfig(
            appOpenAdId = TEST_APP_OPEN_AD_ID,
            bannerAdId = TEST_BANNER_AD_ID,
            nativeAdId = TEST_NATIVE_AD_ID,
            rewardedAdId = TEST_REWARDED_AD_ID,
            interstitialAdId = TEST_INTERSTITIAL_AD_ID,
            enableTestAds = true
        )
    }

    /*fun getAdId(adType: com.example.admonetization.AdType): String {
        return when (adType) {
            com.example.admonetization.AdType.APP_OPEN -> appOpenAdId
            com.example.admonetization.AdType.BANNER -> bannerAdId
            com.example.admonetization.AdType.NATIVE -> nativeAdId
            com.example.admonetization.AdType.REWARDED -> rewardedAdId
            com.example.admonetization.AdType.INTERSTITIAL -> interstitialAdId
        }
    }*/
}