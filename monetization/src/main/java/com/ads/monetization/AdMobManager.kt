package com.ads.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.monetization.banner.BannerAdLoader
import com.ads.monetization.config.AdConfig
import com.ads.monetization.config.AdType
import com.ads.monetization.config.ConsentFromUiResource
import com.ads.monetization.interstitial.InterstitialAdLoader
import com.ads.monetization.native.NativeAdLoader
import com.ads.monetization.openad.AppOpenAdLoader
import com.ads.monetization.rewarded.RewardedAdLoader
import com.androidtech.domain.repository.RemoteConfigRepository
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor(
    private val context: Context,
    private val remoteConfigRepository: RemoteConfigRepository
): AdManager {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var _requestConsentFlow = MutableSharedFlow<ConsentFromUiResource>()
    override val requestConsentFlow = _requestConsentFlow.asSharedFlow()

    private var interstitialAdLoader: InterstitialAdLoader? = null
    private var appOpenAdLoader: AppOpenAdLoader? = null
    private var nativeAdLoader: NativeAdLoader? = null
    private var rewardedAdLoader: RewardedAdLoader? = null
    private var bannerAdLoader: BannerAdLoader? = null

    override fun initializeMobileAdsSdk() {
        applicationScope.launch {
            try {
                MobileAds.initialize(context) { status: com.google.android.gms.ads.initialization.InitializationStatus ->
                    val statusMap = status.adapterStatusMap
                    statusMap.keys.forEach { adapterClass ->
                        val adapterStatus = statusMap[adapterClass]
                        Log.d("MobileAds", "Adapter: $adapterClass status: ${adapterStatus?.initializationState?.name}")
                    }
                }
                MobileAds.setAppVolume(0.5f)
                _requestConsentFlow.emit(ConsentFromUiResource.Complete)
            } catch (exception: Exception) {
                Log.e("MobileAds", "Error initializing MobileAds SDK", exception)
                _requestConsentFlow.emit(ConsentFromUiResource.Complete)
            }
        }
    }

    private suspend fun getAdUnitId(adType: AdType): String {
        val config = remoteConfigRepository.getConfigAd().firstOrNull()
        val adUnitId = when (adType) {
            AdType.INTERSTITIAL -> config?.interAd?.adUnitId
            AdType.APP_OPEN -> config?.appOpenAd?.adUnitId
            AdType.BANNER -> config?.bannerAd?.adUnitId
            AdType.NATIVE -> config?.nativeAd?.adUnitId
            AdType.REWARDED -> config?.rewardedAd?.adUnitId
        }
        return if (!adUnitId.isNullOrBlank()) adUnitId else when (adType) {
            AdType.INTERSTITIAL -> AdConfig.TEST_INTERSTITIAL_AD_ID
            AdType.APP_OPEN -> AdConfig.TEST_APP_OPEN_AD_ID
            AdType.BANNER -> AdConfig.TEST_BANNER_AD_ID
            AdType.NATIVE -> AdConfig.TEST_NATIVE_AD_ID
            AdType.REWARDED -> AdConfig.TEST_REWARDED_AD_ID
        }
    }

    override fun preloadFullScreenAd(
        activity: Activity,
        placeName: String,
        isNeedNotifyEvent: Boolean
    ) {
        preloadAd(activity, AdType.INTERSTITIAL)
        preloadAd(activity, AdType.APP_OPEN)
    }

    override fun preloadAds(activity: Activity) {
        preloadAd(activity, AdType.INTERSTITIAL)
        preloadAd(activity, AdType.APP_OPEN)
        preloadAd(activity, AdType.NATIVE)
    }

    override fun preloadAd(
        activity: Activity,
        adType: AdType
    ) {
        applicationScope.launch {
            val adUnitId = getAdUnitId(adType)
            when (adType) {
                AdType.INTERSTITIAL -> {
                    if (interstitialAdLoader == null) {
                        interstitialAdLoader = InterstitialAdLoader(adUnitId)
                    }
                    interstitialAdLoader?.loadAd(activity)
                }
                AdType.APP_OPEN -> {
                    if (appOpenAdLoader == null) {
                        appOpenAdLoader = AppOpenAdLoader(adUnitId)
                    }
                    appOpenAdLoader?.loadAd(activity)
                }
                AdType.NATIVE -> {
                    if (nativeAdLoader == null) {
                        nativeAdLoader = NativeAdLoader(adUnitId)
                    }
                    nativeAdLoader?.loadAd(activity)
                }
                AdType.REWARDED -> {
                    if (rewardedAdLoader == null) {
                        rewardedAdLoader = RewardedAdLoader(adUnitId)
                    }
                    rewardedAdLoader?.loadAd(activity)
                }
                AdType.BANNER -> {
                    if (bannerAdLoader == null) {
                        bannerAdLoader = BannerAdLoader(adUnitId)
                    }
                }
            }
        }
    }

    override fun showAdIfAvailable(
        activity: Activity,
        adType: AdType,
        onAdShown: () -> Unit,
        onAdFailed: (String) -> Unit,
        onAdClosed: () -> Unit
    ) {
        when (adType) {
            AdType.INTERSTITIAL -> {
                interstitialAdLoader?.showAdIfAvailable(
                    activity = activity,
                    onAdShown = onAdShown,
                    onAdFailed = onAdFailed,
                    onAdDismissed = onAdClosed
                ) ?: onAdFailed("Interstitial loader not initialized")
            }
            AdType.APP_OPEN -> {
                appOpenAdLoader?.showIfAvailable(
                    activity = activity,
                    onAdShown = onAdShown,
                    onAdFailed = onAdFailed,
                    onAdDismissed = onAdClosed
                ) ?: onAdFailed("AppOpen loader not initialized")
            }
            AdType.REWARDED -> {
                rewardedAdLoader?.showAdIfAvailable(
                    activity = activity,
                    onAdShown = onAdShown,
                    onAdFailed = onAdFailed,
                    onAdDismissed = onAdClosed
                ) ?: onAdFailed("Rewarded loader not initialized")
            }
            else -> {
                onAdFailed("Unsupported ad format for full screen show")
            }
        }
    }

    override fun isAdLoaded(adType: AdType): Boolean {
        return when (adType) {
            AdType.INTERSTITIAL -> interstitialAdLoader?.isAdAvailable() == true
            AdType.APP_OPEN -> appOpenAdLoader?.isAdAvailable() == true
            AdType.NATIVE -> nativeAdLoader?.isAdAvailable() == true
            AdType.REWARDED -> rewardedAdLoader?.isAdAvailable() == true
            AdType.BANNER -> bannerAdLoader != null
        }
    }

    override fun clearAd(adType: AdType) {
        when (adType) {
            AdType.INTERSTITIAL -> {
                interstitialAdLoader?.clear()
                interstitialAdLoader = null
            }
            AdType.APP_OPEN -> {
                appOpenAdLoader?.clear()
                appOpenAdLoader = null
            }
            AdType.NATIVE -> {
                nativeAdLoader?.destroy()
                nativeAdLoader = null
            }
            AdType.REWARDED -> {
                rewardedAdLoader?.clear()
                rewardedAdLoader = null
            }
            AdType.BANNER -> {
                bannerAdLoader?.destroy()
                bannerAdLoader = null
            }
        }
    }

    override fun clearAllAds() {
        AdType.entries.forEach { clearAd(it) }
    }
}