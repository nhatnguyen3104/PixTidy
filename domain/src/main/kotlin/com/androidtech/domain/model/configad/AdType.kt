package com.androidtech.domain.model.configad

sealed class AdType {
    abstract val key: String
    abstract val label: String

    companion object {
        fun getAdTypeBy(key: String) = when (key) {
            Banner.key -> Banner
            Native.key -> Native
            Interstitial.key -> Interstitial
            RewardedInterstitial.key -> RewardedInterstitial
            AppOpen.key -> AppOpen
            else -> None
        }
    }

    data object Banner: AdType() {
        override val key: String = "banner"
        override val label: String = "BannerAd"
    }

    data object Native: AdType() {
        override val key: String = "native"
        override val label: String = "NativeAd"
    }

    data object AppOpen: AdType() {
        override val key: String = "app_open"
        override val label: String = "AppOpenAd"
    }

    data object Rewarded: AdType() {
        override val key: String = "rewarded"
        override val label: String = "RewardedAd"
    }

    data object Interstitial: AdType() {
        override val key: String = "interstitial"
        override val label: String = "InterstitialAd"
    }

    data object RewardedInterstitial : AdType() {
        override val key = "rewarded_interstitial"
        override val label: String = "RewardedInterstitialAd"
    }

    data object None : AdType() {
        override val key = "none"
        override val label: String = "NoneAd"
    }
}