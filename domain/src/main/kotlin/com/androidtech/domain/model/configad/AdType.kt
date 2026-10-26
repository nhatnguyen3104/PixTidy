package com.androidtech.domain.model.configad

sealed class AdType {
    abstract val key: String

    data object Banner: AdType() {
        override val key: String = "banner"
    }

    data object Native: AdType() {
        override val key: String = "native"
    }

    data object AppOpen: AdType() {
        override val key: String = "app_open"
    }

    data object Rewarded: AdType() {
        override val key: String = "rewarded"
    }

    data object Interstitial: AdType() {
        override val key: String = "interstitial"
    }

    data object LanguageNative: AdType() {
        override val key: String = "language_native"
    }

    data object SplashBanner: AdType() {
        override val key: String = "splash_banner"
    }
}