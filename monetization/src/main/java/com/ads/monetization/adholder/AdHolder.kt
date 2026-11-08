package com.ads.monetization.adholder

import com.androidtech.data.logger.DataLogger
import com.androidtech.domain.model.configad.AdPlace
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import kotlinx.coroutines.Job

sealed class AdHolder {
    abstract var adPlace: AdPlace
    var subPlaceLevel: Int = 0
    var isLoading: Boolean = false
    var isDelayingToLoad: Boolean = false
    var isShowing: Boolean = false
    var isShowed: Boolean = false
    var isWaitLoadToShow: Boolean = false
    var isNeedNotifyEvent: Boolean = true
    var retryCount: Int = 0
    var needRetry: Boolean = true
    var loadedTimeInSecond: Long = 0L
    var jobDelayingToLoad: Job? = null
    var adNetworkName: String = ""

    /**
     * `true` sau khi đã `notify*Loaded` ở batch load hiện tại — dùng để dedup
     * khi NextGen SDK pre-load N ads vào buffer (theo `adsBufferSize`).
     *
     * Background: với buffer_size > 1, SDK fire `onAdLoaded` callback N lần
     * cho cùng 1 load batch (mỗi buffered ad load → 1 callback). Nếu mỗi
     * callback đều fire UI notify:
     * - Container `removeView + addView` N lần → trigger SDK
     *   `onDetachedFromWindow` → SDK auto-destroy → mất auto-refresh
     * - Visual flicker: banner add 2-N lần sát nhau
     *
     * Đặt ở base [AdHolder] vì NextGen SDK support buffer_size cho **mọi
     * format** (banner, native, ...) — không chỉ banner. Mỗi handler load
     * (BannerRefreshView, NativeAdLoader, ...) check + set field này để
     * chỉ notify lần đầu trong batch.
     *
     * **Reset về `false`** trong `reset()` và `resetState()` để batch
     * tiếp theo (sau khi destroy/release) bắt đầu fresh — nếu không reset,
     * batch sau sẽ KHÔNG notify lần nào → UI không cập nhật.
     */
    var hasNotifiedThisBatch: Boolean = false

    abstract fun reset()

    abstract fun resetState()

    abstract fun isAdLoaded(): Boolean

    abstract fun getTag(): String

}

data class RewardedAdHolder(
    override var adPlace: AdPlace,
    var rewarded: RewardedAd? = null,
    var isEarnedReward: Boolean = false,
    var amount: Int = 0,
): AdHolder() {
    override fun reset() {
        isNeedNotifyEvent = false
        hasNotifiedThisBatch = false
        isDelayingToLoad = false
        jobDelayingToLoad?.cancel()
        isLoading = false
        isWaitLoadToShow = false
        rewarded = null
        isEarnedReward = false
        amount = 0
        retryCount = 0
        needRetry = true
        loadedTimeInSecond = 0
    }

    override fun resetState() {}

    override fun isAdLoaded() = rewarded != null

    override fun getTag() = "Rewarded"
}

data class RewardedInterstitialAdHolder(
    override var adPlace: AdPlace,
    var rewardedInterstitialAd: RewardedInterstitialAd? = null,
    var isEarnedReward: Boolean = false,
    var amount: Int = 0,
): AdHolder() {
    override fun reset() {
        isNeedNotifyEvent = false
        hasNotifiedThisBatch = false
        isDelayingToLoad = false
        jobDelayingToLoad?.cancel()
        isLoading = false
        isWaitLoadToShow = false
        rewardedInterstitialAd = null
        isEarnedReward = false
        amount = 0
        retryCount = 0
        needRetry = true
        loadedTimeInSecond = 0
    }

    override fun resetState() {}

    override fun isAdLoaded() = rewardedInterstitialAd != null

    override fun getTag() = "RewardedInterstitialAd"
}

data class InterstitialAdHolder(
    override var adPlace: AdPlace,
    var interstitialAd: InterstitialAd? = null,
): AdHolder() {
    override fun reset() {
        isNeedNotifyEvent = false
        hasNotifiedThisBatch = false
        isDelayingToLoad = false
        jobDelayingToLoad?.cancel()
        isLoading = false
        isWaitLoadToShow = false
        interstitialAd = null
        retryCount = 0
        needRetry = true
        loadedTimeInSecond = 0
    }

    override fun resetState() {}

    override fun isAdLoaded() = interstitialAd != null

    override fun getTag() = "InterstitialAd"
}

data class BannerAdHolder(
    override var adPlace: AdPlace,
    var bannerAd: AdView? = null,
//    var bannerRefreshView: BannerRefreshView? = null,
    var isCanShowBannerCollapsibleIfNeed: Boolean = true,
    var collapsibleShowCount: Int = 0,
    var isLoadSuccess: Boolean = false,
    var jobNotifyBannerLoaded: Job? = null,
): AdHolder() {
    override fun reset() {
        isNeedNotifyEvent = false
        hasNotifiedThisBatch = false
        if (!isShowed && !isLoading) {
            DataLogger.d("${getTag()} ${adPlace.placeName} reset skipped: not shown yet")
            return
        }
        DataLogger.d("${getTag()} ${adPlace.placeName} reset")
        isDelayingToLoad = false
        jobDelayingToLoad?.cancel()
        jobNotifyBannerLoaded?.cancel()
        isLoading = false
        isShowed = false
        collapsibleShowCount = 0
        isWaitLoadToShow = false
        bannerAd = null
//        bannerRefreshView = null
        retryCount = 0
        isLoadSuccess = false
//        needRetry = true // native don't need reset this field
        loadedTimeInSecond = 0
    }

    override fun resetState() {
        DataLogger.d("${getTag()} ${adPlace.placeName} resetState (state-only, no destroy)")
        isNeedNotifyEvent = false
        hasNotifiedThisBatch = false
        isDelayingToLoad = false
        jobDelayingToLoad?.cancel()
        jobNotifyBannerLoaded?.cancel()
        isLoading = false
        isShowed = false
        collapsibleShowCount = 0
        isWaitLoadToShow = false
        bannerAd = null
//        bannerRefreshView = null
        retryCount = 0
        isLoadSuccess = false
        loadedTimeInSecond = 0
    }

    override fun isAdLoaded() = isLoadSuccess

    override fun getTag() = "BannerAd"
}

data class NativeAdHolder(
    override var adPlace: AdPlace,
    var nativeAd: NativeAd? = null,
): AdHolder() {
    override fun reset() {
        isNeedNotifyEvent = false
        hasNotifiedThisBatch = false
        if (!isShowed) {
            DataLogger.d("${getTag()} ${adPlace.placeName} reset skipped: not shown yet nativeAd=$nativeAd")
            return
        }
        DataLogger.d("${getTag()} ${adPlace.placeName} reset nativeAd=$nativeAd")
        isDelayingToLoad = false
        jobDelayingToLoad?.cancel()
        isLoading = false
        isShowed = false
        isWaitLoadToShow = false
        nativeAd?.destroy()
        nativeAd = null
        retryCount = 0
        needRetry = true // native don't need reset this field
        loadedTimeInSecond = 0
    }

    override fun resetState() {
        DataLogger.d("${getTag()} ${adPlace.placeName} resetState nativeAd=$nativeAd")
        isNeedNotifyEvent = false
        hasNotifiedThisBatch = false
        isDelayingToLoad = false
        jobDelayingToLoad?.cancel()
        isLoading = false
        isShowed = false
        isWaitLoadToShow = false
        nativeAd?.destroy()
        nativeAd = null
        retryCount = 0
//        needRetry = true // native don't need reset this field
        loadedTimeInSecond = 0
    }

    override fun isAdLoaded() = nativeAd != null

    override fun getTag() = "NativeAd"
}

data class AppOpenAdHolder(
    override var adPlace: AdPlace,
    var appOpenAd: AppOpenAd? = null,
): AdHolder() {
    override fun reset() {
        isNeedNotifyEvent = false
        hasNotifiedThisBatch = false
        isDelayingToLoad = false
        jobDelayingToLoad?.cancel()
        isLoading = false
        isWaitLoadToShow = false
        appOpenAd = null
        retryCount = 0
        loadedTimeInSecond = 0
    }

    override fun resetState() {}

    override fun isAdLoaded() = appOpenAd != null

    override fun getTag() = "AppOpenAd"

    fun isAdAvailable(): Boolean {
        return appOpenAd != null
    }
}