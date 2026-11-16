package com.ads.monetization

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import com.androidtech.base.R
import com.androidtech.base.extension.getBannerAdWidth
import com.androidtech.base.extension.getBannerAdWidthByContext
import com.androidtech.base.extension.getScreenHeightDp
import com.androidtech.base.extension.getScreenHeightDpByContext
import com.androidtech.base.extension.getScreenWidthDp
import com.androidtech.base.extension.getScreenWidthDpByContext
import com.androidtech.base.prefs.PreferenceHelper
import com.androidtech.data.logger.DataLogger
import com.androidtech.data.model.aduiresource.AdErrorType
import com.androidtech.data.model.aduiresource.AdFullScreenUiResource
import com.androidtech.data.model.aduiresource.AdLoadBannerNativeUiResource
import com.androidtech.data.model.configads.PreventShowManyInterstitialAds
import com.androidtech.data.model.mapper.mapperModel
import com.androidtech.data.remoteconfig.RemoteConfigService
import com.androidtech.data.utils.findActivity
import com.androidtech.data.utils.getCountryCode
import com.androidtech.data.utils.getCurrentTimeInSecond
import com.androidtech.domain.model.configad.AdPlace
import com.androidtech.domain.model.configad.AdType
import com.androidtech.domain.model.configad.AdUnit
import com.androidtech.domain.model.configad.BannerAdPlace
import com.androidtech.domain.model.configad.BannerSize
import com.androidtech.domain.model.configad.ConfigPreventAdClick
import com.androidtech.domain.model.configad.NativeAdPlace
import com.androidtech.domain.model.configad.NativeTemplateType
import com.androidtech.domain.repository.RemoteConfigRepository
import com.ads.monetization.adholder.AdHolder
import com.ads.monetization.adholder.BannerAdHolder
import com.ads.monetization.adholder.InterstitialAdHolder
import com.ads.monetization.adholder.NativeAdHolder
import com.ads.monetization.adholder.RewardedAdHolder
import com.ads.monetization.adholder.RewardedInterstitialAdHolder
import com.ads.monetization.config.ConsentFromUiResource
import com.androidtech.base.BuildConfig
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRefreshCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesPlacement
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdSourceResponseInfo
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.common.VideoOptions
import com.google.android.libraries.ads.mobile.sdk.initialization.AdapterStatus
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdPreloader
import com.androidtech.data.model.aduiresource.getAdPlaceName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class AdMobManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val remoteConfigService: RemoteConfigService,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val preference: PreferenceHelper
) : AdManager, Application.ActivityLifecycleCallbacks {

    private var currentActivity: Activity? = null

    init {
        (context as? Application)?.registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    companion object {
        private const val ONE_HOUR_IN_SECOND = 55 * 60L
        private const val ELIGIBLE_TIME_TO_PRELOAD_FULLSCREEN_ADS = 5

        /** Số ad giữ sẵn trong buffer của preloader cho mỗi rewarded ad unit. */
        private const val PRELOAD_BUFFER_SIZE = 1

        /** Thời gian tối đa chờ ad khi isWaitLoadToShow = true. */
        private const val WAIT_LOAD_TIMEOUT_MS = 10_000L
        private const val FULL_SCREEN_AD_POLL_INTERVAL_MS = 200L

    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var _requestConsentFlow = MutableSharedFlow<ConsentFromUiResource>()
    override val requestConsentFlow = _requestConsentFlow.asSharedFlow()

    private val _adLoadBannerNativeFlow = MutableSharedFlow<AdLoadBannerNativeUiResource>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val adBannerNativeFlow = _adLoadBannerNativeFlow.asSharedFlow()

    private val _adFullScreenFlow = MutableSharedFlow<AdFullScreenUiResource>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val adFullScreenFlow = _adFullScreenFlow.asSharedFlow()

    private val adHolderMap = mutableMapOf<String, AdHolder>()
    private val adHolderFullScreenMap = mutableMapOf<String, AdHolder>()

    private data class PendingBannerNativePreload(
        val placeName: String,
        val subPlaceLevel: Int,
        val isCanShowBannerCollapsibleIfNeed: Boolean,
        val isPreloadForTheSameActivity: Boolean
    )

    private data class PendingFullScreenPreload(
        val activity: Activity,
        val placeName: String
    )

    private val pendingBannerNativePreloads = mutableSetOf<PendingBannerNativePreload>()
    private val pendingFullScreenPreloads = mutableSetOf<PendingFullScreenPreload>()

    private val countryCode by lazy { context.getCountryCode() }

    private var fullScreenAdWaitLoadJob: Job? = null

    override fun initializeMobileAdsSdk() {
        applicationScope.launch(Dispatchers.Main) {
            try {
                val appId = BuildConfig.admob_app_id
                MobileAds.initialize(
                    context,
                    InitializationConfig.Builder(appId).build()
                ) { status ->
                    DataLogger.d("initializeMobileAdsSdk complete: ${status.adapterStatusMap}")
                    val statusMap = status.adapterStatusMap
                    for ((adapterClass, status) in statusMap) {
                        when (status.initializationState) {
                            AdapterStatus.InitializationState.COMPLETE -> {
                                DataLogger.d( "AdMobMediation Adapter $adapterClass đã sẵn sàng.")
                            }
                            AdapterStatus.InitializationState.FAILED -> {
                                DataLogger.e( "AdMobMediation Adapter $adapterClass chưa sẵn sàng.")
                            }

                            else -> {}
                        }
                    }

                    applicationScope.launch {
                        _requestConsentFlow.emit(ConsentFromUiResource.Complete)
                        processPendingPreloads()
                    }
                }
            } catch (exception: Throwable) {
                DataLogger.e("initializeMobileAdsSdk failed", exception)
                _requestConsentFlow.emit(ConsentFromUiResource.Complete)
            }
        }
    }

    private fun processPendingPreloads() {
        applicationScope.launch(Dispatchers.Main) {
            val pendingNative = pendingBannerNativePreloads.toList()
            pendingBannerNativePreloads.clear()
            pendingNative.forEach { pending ->
                preloadBannerNativeAd(
                    placeName = pending.placeName,
                    subPlaceLevel = pending.subPlaceLevel,
                    isCanShowBannerCollapsibleIfNeed = pending.isCanShowBannerCollapsibleIfNeed,
                    isPreloadForTheSameActivity = pending.isPreloadForTheSameActivity
                )
            }

            val pendingFull = pendingFullScreenPreloads.toList()
            pendingFullScreenPreloads.clear()
            pendingFull.forEach { pending ->
                preloadFullScreenAd(
                    activity = pending.activity,
                    placeName = pending.placeName
                )
            }
        }
    }

    override fun getConfigCountriesDisableAds(): List<String> {
        return remoteConfigService.getConfigCountriesDisableAds() ?: emptyList()
    }

    override fun getConfigPreventAdClick(): ConfigPreventAdClick {
        return remoteConfigService.getConfigPreventAdClick()?.mapperModel()
            ?: ConfigPreventAdClick.default
    }

    override fun startDisableAdCountDownTimer() {
        TODO("Not yet implemented")
    }

    override fun isNotAbleToVisibleAdsToUser(placeName: String): Boolean {
        if (isCountryNotAvailableToShowAd()) return true
        if (isPreventShowAdsDueManyAdsClicked()) return true
        return false
    }

    override fun isCannotShowInterAd(adPlace: AdPlace): Boolean {
        if (adPlace.isIgnoreInterval) {
            return false
        }

        if (PreventShowManyInterstitialAds.isNotValidIntervalTimeShowAds()) {
            return true
        }

        if (PreventShowManyInterstitialAds.isNotValidSessionTimeShowAds(remoteConfigRepository.getConfigAdsInterstitial())) {
            return true
        }
        return false
    }

    override fun getAdUnit(adPlace: AdPlace): AdUnit {
        val now = getCurrentTimeInSecond()
        val (label, adUnit) = when {
            adPlace.isBannerType() -> when {
                else -> AdType.Banner.label to adPlace.getValueAdId()
            }

            adPlace.isNativeType() -> when {
                else -> AdType.Native.label to adPlace.getValueAdId()
            }

            adPlace.isInterstitialType() -> when {
                else -> AdType.Interstitial.label to adPlace.getValueAdId()
            }

            adPlace.isRewardedInterstitialType() -> when {
                else -> AdType.RewardedInterstitial.label to adPlace.getValueAdId()
            }

            adPlace.isAppOpenType() -> when {
                else -> AdType.AppOpen.label to adPlace.getValueAdId()
            }

            else -> AdType.None.label to adPlace.getValueAdId()
        }
        val tier = when (adUnit) {
            adPlace.adIdHigh -> "H"
            adPlace.adIdMedium -> "M"
            else -> "D"
        }
        DataLogger.d("$label ${adPlace.placeName} adUnit tier=$tier adId=${adUnit.adUnitId}")
        return adUnit
    }

    override fun getAdPlaceBy(adPlaceName: String): AdPlace {
        return remoteConfigRepository.getAdPlaceBy(adPlaceName)
    }

    override fun getAdRequest(adId: String): AdRequest {
        return AdRequest.Builder(adId)
            .build()
    }

    override fun getUniqueAdSourceName(loadedAdapterResponseInfo: AdSourceResponseInfo?): String {
        return loadedAdapterResponseInfo?.name ?: ""
    }

    override fun preloadFullScreenAd(activity: Activity, placeName: String) {
        DataLogger.d("→ preloadFullScreenAd $placeName")
        if (!MobileAds.isInitialized) {
            DataLogger.d("preloadFullScreenAd $placeName queued: SDK not initialized yet")
            pendingFullScreenPreloads.add(PendingFullScreenPreload(activity, placeName))
            return
        }
        if (!remoteConfigService.getRemoteConfigAdsActive()) {
            DataLogger.d("preloadFullScreenAd: AdActive = false")
            notifyAdFullScreenNotValidOrLoadFailed(getAdPlaceBy(placeName))
            return
        }
        val adPlace = getAdPlaceBy(placeName)
        DataLogger.d("PreloadFullAd: ${adPlace.adType}")

        if (adPlace.isBannerOrNativePlace()) {
            return
        }

        val adHolder = getOrCreateFullscreenAdHolderBy(adPlace)
        adHolder.isWaitLoadToShow = false
        if (adHolder.adPlace.isIgnoreInterval) {
            adHolder.jobDelayingToLoad?.cancel()
            adHolder.isDelayingToLoad = false
        }
        if (adHolder.isDelayingToLoad) {
            DataLogger.d("InterstitialAd $placeName loadIfNeed skipped: already delaying")
            return
        }
        if (adPlace.isInterstitialType()) {
            if (isNotAbleToVisibleAdsToUser(placeName)) {
                notifyAdFullScreenNotValidOrLoadFailed(adPlace)
                return
            }
            val periodOfTimeWithoutShowingAds =
                getCurrentTimeInSecond() - PreventShowManyInterstitialAds.getLastTimeShowedInterAd()
            var intervalTimeToLoadAds =
                remoteConfigRepository.getConfigAdsInterstitial().timeInterval - ELIGIBLE_TIME_TO_PRELOAD_FULLSCREEN_ADS - periodOfTimeWithoutShowingAds

            DataLogger.d("InterstitialAd $placeName interval check: idleSec=$periodOfTimeWithoutShowingAds delaySec=$intervalTimeToLoadAds")

            if (intervalTimeToLoadAds < 0) intervalTimeToLoadAds = 0L
            applicationScope.launch(Dispatchers.IO) {
                DataLogger.d("InterstitialAd $placeName delaying ${intervalTimeToLoadAds}s before load")
                delay((intervalTimeToLoadAds * 1000L).milliseconds)
                loadInterstitialIfNeed(activity, adHolder as InterstitialAdHolder)
            }
            return
        }

        if (adPlace.isRewardedInterstitialType()) {
            if (isNotAbleToVisibleAdsToUser(adPlace.placeName)) {
                notifyAdFullScreenNotValidOrLoadFailed(adPlace)
                return
            }
            preloadRewardedInterstitialNew(adHolder as RewardedInterstitialAdHolder)
            return
        }

        if (adPlace.isRewardedType()) {
            if (isNotAbleToVisibleAdsToUser(adPlace.placeName)) {
                notifyAdFullScreenNotValidOrLoadFailed(adPlace)
                return
            }
            preloadRewarded(adHolder as RewardedAdHolder)
        }
    }

    override fun preloadBannerNativeAd(
        placeName: String,
        subPlaceLevel: Int,
        isCanShowBannerCollapsibleIfNeed: Boolean,
        isPreloadForTheSameActivity: Boolean
    ) {
        DataLogger.d("→ preloadBannerNative $placeName subPlace=$subPlaceLevel sameActivity=$isPreloadForTheSameActivity")
        if (!MobileAds.isInitialized) {
            DataLogger.d("← preloadBannerNative $placeName queued: SDK not initialized yet")
            pendingBannerNativePreloads.add(
                PendingBannerNativePreload(
                    placeName = placeName,
                    subPlaceLevel = subPlaceLevel,
                    isCanShowBannerCollapsibleIfNeed = isCanShowBannerCollapsibleIfNeed,
                    isPreloadForTheSameActivity = isPreloadForTheSameActivity
                )
            )
            return
        }
        if (!remoteConfigService.getRemoteConfigAdsActive()) {
            DataLogger.d("preloadBannerNative: AdActive = false")
            notifyAdFullScreenNotValidOrLoadFailed(getAdPlaceBy(placeName))
            return
        }
        if (!isPreloadForTheSameActivity) {
            DataLogger.d("← preloadBannerNative $placeName skipped: collapsible (cross-activity)")
            return
        }
        loadBannerNativeAd(placeName, subPlaceLevel, isCanShowBannerCollapsibleIfNeed)
    }

    override fun showFullScreenAd(activity: Activity, placeName: String) {
        DataLogger.d("→ showFullScreenAd $placeName")
        if (!MobileAds.isInitialized) {
            DataLogger.d("← showFullScreenAd $placeName skipped: SDK not initialized")
            notifyAdFullScreenNotValidOrLoadFailed(getAdPlaceBy(placeName))
            return
        }
        if (!remoteConfigService.getRemoteConfigAdsActive()) {
            DataLogger.d("showFullScreenAd: AdActive = false")
            notifyAdFullScreenNotValidOrLoadFailed(getAdPlaceBy(placeName))
            return
        }
        val adPlace = getAdPlaceBy(placeName)
        if (isNotAbleToVisibleAdsToUser(placeName)) {
            notifyAdFullScreenCompleted(
                adPlace = adPlace,
                isEarnedReward = true,
                amount = 0,
                adErrorType = AdErrorType.None
            )
            return
        }
        if (adPlace.isBannerOrNativePlace()) {
            notifyAdFullScreenCompleted(
                adPlace = adPlace,
                isEarnedReward = true,
                amount = 0,
                adErrorType = AdErrorType.None
            )
            return
        }
        val adHolder = getOrCreateFullscreenAdHolderBy(adPlace)
        if (adPlace.isInterstitialType()) {
            if (isNotAbleToVisibleAdsToUser(placeName)) {
                notifyAdFullScreenCompleted(
                    adPlace = adPlace,
                    isEarnedReward = true,
                    amount = 0,
                    adErrorType = AdErrorType.None
                )
                return
            }
            adHolder.isWaitLoadToShow =
                remoteConfigRepository.getConfigAdsInterstitial().isWaitLoadToShow
            adHolder.adPlace = adPlace
            showInterstitial(
                activity = activity,
                adHolder = adHolder as InterstitialAdHolder,
            )
            return
        }
        if (adPlace.isRewardedInterstitialType()) {
            if (isNotAbleToVisibleAdsToUserRewardedAndRewardedInterstitial(placeName)) {
                notifyAdFullScreenCompleted(
                    adPlace = adPlace,
                    isEarnedReward = false,
                    amount = 0,
                    adErrorType = AdErrorType.None
                )
                return
            }
            adHolder.isWaitLoadToShow =
                remoteConfigRepository.getConfigAdsRewardedInterstitial().isWaitLoadToShow
            adHolder.adPlace = adPlace
            showRewardedInterstitialVideo(
                activity = activity,
                adHolder = adHolder as RewardedInterstitialAdHolder,
            )
            return
        }

        if (adPlace.isRewardedType()) {
            if (isNotAbleToVisibleAdsToUserRewardedAndRewardedInterstitial(placeName)) {
                notifyAdFullScreenCompleted(
                    adPlace = adPlace,
                    isEarnedReward = false,
                    amount = 0,
                    adErrorType = AdErrorType.None
                )
                return
            }
            adHolder.isWaitLoadToShow =
                remoteConfigRepository.getConfigAdsRewarded().isWaitLoadToShow
            adHolder.adPlace = adPlace
            showRewardedVideo(
                activity = activity,
                adHolder = adHolder as RewardedAdHolder,
            )
            return
        }
        notifyAdFullScreenCompleted(
            adPlace = adPlace,
            isEarnedReward = true,
            amount = 0,
            adErrorType = AdErrorType.None
        )
    }

    override fun showBannerNativeAd(
        placeName: String,
        subPlaceLevel: Int,
        isCanShowBannerCollapsibleIfNeed: Boolean
    ) {
        if (!MobileAds.isInitialized) {
            notifyAdFullScreenNotValidOrLoadFailed(getAdPlaceBy(placeName))
            return
        }
        if (!remoteConfigService.getRemoteConfigAdsActive()) {
            notifyAdFullScreenNotValidOrLoadFailed(getAdPlaceBy(placeName))
            return
        }
        loadBannerNativeAd(
            placeName = placeName,
            subPlaceLevel = subPlaceLevel,
            isCanShowBannerCollapsibleIfNeed = isCanShowBannerCollapsibleIfNeed
        )
    }

    override fun releaseBannerNative(
        placeName: String,
        subPlaceLevel: Int
    ) {
        DataLogger.d("→ releaseBannerNative $placeName subPlace=$subPlaceLevel")
        val adPlace = getAdPlaceBy(placeName)
        val adHolder = getOrCreateBannerNativeAdHolderBy(
            adPlace,
            subPlaceLevel
        )
        if (adHolder is BannerAdHolder && !adHolder.isShowed && adHolder.isLoading) {
            val staleBannerAd = adHolder.bannerAd
            adHolder.resetState()
            Handler(Looper.getMainLooper()).post {
                try {
                    staleBannerAd?.destroy()
                } catch (_: Exception) {
                }
            }
        } else {
            adHolder.reset()
        }
        adHolder.needRetry = false
    }

    override fun getBannerAdHolder(
        placeName: String,
        subPlaceLevel: Int
    ): BannerAdHolder? {
        if (!remoteConfigService.getRemoteConfigAdsActive()) {
            DataLogger.d("getBannerAdHolder: AdActive = false")
            notifyAdFullScreenNotValidOrLoadFailed(getAdPlaceBy(placeName))
            return null
        }
        val adPlace = getAdPlaceBy(placeName)
        val adHolder = getOrCreateBannerNativeAdHolderBy(adPlace, subPlaceLevel)
        return adHolder as? BannerAdHolder
    }

    override fun getNativeAdHolder(placeName: String, subPlaceLevel: Int): NativeAdHolder? {
        if (!remoteConfigService.getRemoteConfigAdsActive()) {
            DataLogger.d("getNativeAdHolder: AdActive = false")
            notifyAdFullScreenNotValidOrLoadFailed(getAdPlaceBy(placeName))
            return null
        }
        val adPlace = getAdPlaceBy(placeName)
        val adHolder = getOrCreateBannerNativeAdHolderBy(adPlace, subPlaceLevel)
        return adHolder as? NativeAdHolder
    }

    override fun observeBannerNativeAd(
        placeName: String,
        subPlaceLevel: Int
    ): Flow<AdLoadBannerNativeUiResource> {
        return adBannerNativeFlow
            .filter { resource ->
                resource.getAdPlaceName() == placeName
            }
            .onStart {
                val adPlace = getAdPlaceBy(placeName)
                val holder = adHolderMap[getAdHolderMapKey(adPlace, subPlaceLevel)]
                if (holder is NativeAdHolder && holder.nativeAd != null) {
                    emit(
                        AdLoadBannerNativeUiResource.NativeAdLoaded(
                            nativeAd = holder.nativeAd!!,
                            nativeAdPlace = holder.adPlace as NativeAdPlace,
                            subPlaceLevel = holder.subPlaceLevel
                        )
                    )
                } else if (holder is BannerAdHolder && holder.bannerAd != null) {
                    emit(
                        AdLoadBannerNativeUiResource.BannerAdLoaded(
                            bannerAd = holder.bannerAd!!,
                            bannerAdPlace = holder.adPlace as BannerAdPlace,
                            subPlaceLevel = holder.subPlaceLevel
                        )
                    )
                }
            }
    }

    override fun observeFullScreenAd(
        placeName: String
    ): Flow<AdFullScreenUiResource> {
        return adFullScreenFlow
            .filter { resource ->
                resource.getAdPlaceName() == placeName
            }
            .onStart {
                val adPlace = getAdPlaceBy(placeName)
                val holder = adHolderFullScreenMap[adPlace.adUnit.adUnitId]
                if (holder != null && holder.isAdLoaded()) {
                    emit(AdFullScreenUiResource.AdLoaded(adPlace))
                }
            }
    }

    private fun isCountryNotAvailableToShowAd(): Boolean {
        return remoteConfigRepository.getConfigCountriesDisableAds().contains(countryCode)
    }

    private fun isPreventShowAdsDueManyAdsClicked() =
        getCurrentTimeInSecond() < preference.timeOfFirstAdClicked

    private fun notifyAdFullScreenRequestShowing(adPlace: AdPlace) {
        applicationScope.launch {
            _adFullScreenFlow.emit(AdFullScreenUiResource.AdRequestInfo(adPlace))
        }
    }


    private fun handleWhenAdFullscreenLoaded(
        activity: Activity,
        adHolder: AdHolder,
    ) {
        val adPlace = adHolder.adPlace
        val placeName = adPlace.placeName
        adHolder.isLoading = false
//        cancelWaitLoadTimeout(placeName)
        notifyAdFullScreenLoaded(adPlace)
    }

    private fun notifyAdFullScreenNotValidOrLoadFailed(adPlace: AdPlace) {
        applicationScope.launch {
            _adFullScreenFlow.emit(AdFullScreenUiResource.AdNotValidOrLoadFailed(adPlace))
        }
    }

    private fun notifyAdFullScreenFailedToShow(adPlace: AdPlace, canRetryToShow: Boolean) {
        applicationScope.launch {
            _adFullScreenFlow.emit(
                AdFullScreenUiResource.AdFailedToShow(
                    adPlace,
                    canRetryToShow
                )
            )
        }
    }

    private fun notifyAdFullScreenLoaded(adPlace: AdPlace) {
        applicationScope.launch {
            _adFullScreenFlow.emit(AdFullScreenUiResource.AdLoaded(adPlace))
        }
    }

    private fun notifyAdFullScreenSucceedToShow(adPlace: AdPlace) {
        applicationScope.launch {
            _adFullScreenFlow.emit(AdFullScreenUiResource.AdSucceedToShow(adPlace))
        }
    }

    private fun notifyAdFullScreenCompleted(
        adPlace: AdPlace,
        isEarnedReward: Boolean,
        amount: Int,
        adErrorType: AdErrorType
    ) {
        applicationScope.launch {
            _adFullScreenFlow.emit(
                AdFullScreenUiResource.AdCompleted(
                    adPlace = adPlace,
                    isEarnedReward = isEarnedReward,
                    amount = amount,
                    adErrorType = adErrorType
                )
            )
        }
    }

    private fun loadInterstitialIfNeed(activity: Activity, adPlaceHolder: InterstitialAdHolder) {
        val loadCallback = object : AdLoadCallback<InterstitialAd> {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                super.onAdFailedToLoad(adError)
                DataLogger.d("onAdFailedToLoad code=${adError.code} msg=${adError.message}")
                // Need to handle failed to load Interstitial
            }

            override fun onAdLoaded(ad: InterstitialAd) {
                super.onAdLoaded(ad)
                DataLogger.d("loadInterstitialIfNeed onAdLoaded")
                adPlaceHolder.interstitialAd = ad
                handleWhenAdFullscreenLoaded(activity, adPlaceHolder)
            }
        }

        try {
            InterstitialAd.load(
                getAdRequest(getAdUnit(adPlaceHolder.adPlace).adUnitId),
                loadCallback
            )
        } catch (e: Exception) {
            notifyAdFullScreenNotValidOrLoadFailed(adPlaceHolder.adPlace)
        }
    }

    private fun preloadRewardedInterstitial(adHolder: RewardedInterstitialAdHolder) {
        val adRequest = AdRequest.Builder(adHolder.adPlace.adUnit.adUnitId).build()
        val preloadConfig = PreloadConfiguration(adRequest)
        RewardedInterstitialAdPreloader.start(adHolder.adPlace.adUnit.adUnitId, preloadConfig)
    }

    private fun preloadRewardedInterstitialNew(
        adHolder: RewardedInterstitialAdHolder
    ) {
        val adUnitId = getAdUnit(adHolder.adPlace).adUnitId
        if (adUnitId.isBlank()) {
            DataLogger.e("preloadRewardedInterstitial ${adHolder.adPlace.placeName} skipped: empty adUnitId")
            notifyAdFullScreenNotValidOrLoadFailed(adHolder.adPlace)
            return
        }
        if (RewardedInterstitialAdPreloader.isAdAvailable(adUnitId)) {
            DataLogger.d("preloadRewardedInterstitial ${adHolder.adPlace.placeName} skipped: ad already available")
            adHolder.isLoading = false
            notifyAdFullScreenLoaded(adHolder.adPlace)
            return
        }
        val preloadConfig = PreloadConfiguration(
            AdRequest.Builder(adUnitId).build(),
            PRELOAD_BUFFER_SIZE
        )
        adHolder.isLoading = true
        val started = RewardedInterstitialAdPreloader.start(
            adUnitId,
            preloadConfig,
            object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    super.onAdPreloaded(preloadId, responseInfo)
                    DataLogger.d("RewardedInterstitial ${adHolder.adPlace.placeName} onAdPreloaded id=$preloadId")
                    adHolder.isLoading = false
                    adHolder.retryCount = 0
                    adHolder.loadedTimeInSecond = getCurrentTimeInSecond()
                    adHolder.adNetworkName =
                        getUniqueAdSourceName(responseInfo.loadedAdSourceResponseInfo)

                    if (adHolder.hasNotifiedThisBatch) return
                    adHolder.hasNotifiedThisBatch = true
                    notifyAdFullScreenLoaded(adHolder.adPlace)
                }

                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    super.onAdFailedToPreload(preloadId, adError)
                    DataLogger.e(
                        "RewardedInterstitial ${adHolder.adPlace.placeName} onAdFailedToPreload " +
                                "code=${adError.code} msg=${adError.message}"
                    )
                    adHolder.isLoading = false
                    notifyAdFullScreenNotValidOrLoadFailed(adHolder.adPlace)
                }

                override fun onAdsExhausted(preloadId: String) {
                    super.onAdsExhausted(preloadId)
                    DataLogger.d("RewardedInterstitial ${adHolder.adPlace.placeName} onAdsExhausted id=$preloadId")
                    adHolder.hasNotifiedThisBatch = false
                }
            }
        )
        if (!started) {
            DataLogger.d("preloadRewarded ${adHolder.adPlace.placeName} start() returned false")
            adHolder.isLoading = false
        }
    }

    private fun showRewardedInterstitialAdInternal(
        activity: Activity,
        adHolder: RewardedInterstitialAdHolder,
        rewardedInterstitialAd: RewardedInterstitialAd
    ) {
        val adPlace = adHolder.adPlace
        adHolder.rewardedInterstitialAd = rewardedInterstitialAd
        adHolder.isEarnedReward = false
        adHolder.amount = 0
        adHolder.isShowing = true
        adHolder.isWaitLoadToShow = false

        rewardedInterstitialAd.setImmersiveMode(true)
        rewardedInterstitialAd.adEventCallback = object : RewardedInterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                DataLogger.d("Rewarded ${adPlace.placeName} showed")
                adHolder.isShowed = true
                notifyAdFullScreenSucceedToShow(adPlace)
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                DataLogger.e(
                    "Rewarded ${adPlace.placeName} failed to show " +
                            "code=${fullScreenContentError.code} msg=${fullScreenContentError.message}"
                )
                // Ad hỏng → bỏ hẳn, nạp lại buffer, và báo UI (cho phép retry).
                finishRewardedInterstitialSession(adHolder)
                notifyAdFullScreenFailedToShow(adPlace, true)
                notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.Other)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                DataLogger.d("Rewarded ${adPlace.placeName} impression")
            }

            override fun onAdClicked() {
                super.onAdClicked()
                DataLogger.d("Rewarded ${adPlace.placeName} clicked")
            }

            override fun onAdPaid(value: AdValue) {
                super.onAdPaid(value)
                DataLogger.d(
                    "Rewarded ${adPlace.placeName} paid " +
                            "${value.valueMicros} ${value.currencyCode}"
                )
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                // Chốt kết quả reward đúng 1 lần, tại thời điểm đóng ad.
                val isEarnedReward = adHolder.isEarnedReward
                val amount = adHolder.amount
                DataLogger.d(
                    "Rewarded ${adPlace.placeName} dismissed earned=$isEarnedReward amount=$amount"
                )
                finishRewardedInterstitialSession(adHolder)
                notifyAdFullScreenCompleted(
                    adPlace = adPlace,
                    isEarnedReward = isEarnedReward,
                    amount = amount,
                    adErrorType = AdErrorType.None
                )
            }
        }

        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) {
                DataLogger.e("Rewarded ${adPlace.placeName} abort show: activity gone")
                finishRewardedInterstitialSession(adHolder)
                notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.Other)
                return@runOnUiThread
            }
            rewardedInterstitialAd.show(activity) { rewardItem ->
                // Chỉ ghi nhận, KHÔNG notify ở đây — chờ onAdDismissed để tránh
                // UI mở màn reward trong khi ad vẫn đang hiển thị.
                adHolder.isEarnedReward = true
                adHolder.amount = rewardItem.amount
                DataLogger.d(
                    "Rewarded ${adPlace.placeName} earned ${rewardItem.amount} ${rewardItem.type}"
                )
            }
        }
    }

    private fun showInterstitial(
        activity: Activity,
        adHolder: InterstitialAdHolder
    ) {
        if (isCannotShowInterAd(adHolder.adPlace)) {
            notifyAdFullScreenCompleted(
                adPlace = adHolder.adPlace,
                isEarnedReward = true,
                amount = 0,
                adErrorType = AdErrorType.None
            )
            return
        }
        val interstitialAd = adHolder.interstitialAd
        if (interstitialAd != null) {
            adHolder.isShowing = true
            interstitialAd.setImmersiveMode(true)
            interstitialAd.adEventCallback = object : InterstitialAdEventCallback {
                override fun onAdShowedFullScreenContent() {
                    // Interstitial ad did show.
                }

                override fun onAdDismissedFullScreenContent() {
                    // Interstitial ad did dismiss.
                    adHolder.interstitialAd = null
                    notifyAdFullScreenCompleted(adHolder.adPlace, false, 0, AdErrorType.None)
                }

                override fun onAdFailedToShowFullScreenContent(
                    fullScreenContentError: FullScreenContentError
                ) {
                    // Interstitial ad failed to show.
                }

                override fun onAdImpression() {
                    // Interstitial ad did record an impression.
                }

                override fun onAdClicked() {
                    // Interstitial ad did record a click.
                }
            }
            interstitialAd.show(activity)
        }
    }

    private fun preloadRewarded(adHolder: RewardedAdHolder) {
        val adUnitId = getAdUnit(adHolder.adPlace).adUnitId
        if (adUnitId.isBlank()) {
            DataLogger.e("preloadRewarded ${adHolder.adPlace.placeName} skipped: empty adUnitId")
            notifyAdFullScreenNotValidOrLoadFailed(adHolder.adPlace)
            return
        }
        // Preloader đã chạy và còn ad trong buffer → không start lại (tránh reset buffer)
        if (RewardedAdPreloader.isAdAvailable(adUnitId)) {
            DataLogger.d("preloadRewarded ${adHolder.adPlace.placeName} skipped: ad already available")
            adHolder.isLoading = false
            notifyAdFullScreenLoaded(adHolder.adPlace)
            return
        }

        val preloadConfig = PreloadConfiguration(
            AdRequest.Builder(adUnitId).build(),
            PRELOAD_BUFFER_SIZE
        )
        adHolder.isLoading = true
        val started = RewardedAdPreloader.start(
            adUnitId,
            preloadConfig,
            object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    super.onAdPreloaded(preloadId, responseInfo)
                    DataLogger.d("Rewarded ${adHolder.adPlace.placeName} onAdPreloaded id=$preloadId")
                    adHolder.isLoading = false
                    adHolder.retryCount = 0
                    adHolder.loadedTimeInSecond = getCurrentTimeInSecond()
                    adHolder.adNetworkName =
                        getUniqueAdSourceName(responseInfo.loadedAdSourceResponseInfo)
                    // KHÔNG set adHolder.rewarded ở đây: ad vẫn nằm trong buffer của
                    // preloader, chỉ pollAd() lúc show mới lấy ra được.
                    if (adHolder.hasNotifiedThisBatch) return
                    adHolder.hasNotifiedThisBatch = true
                    notifyAdFullScreenLoaded(adHolder.adPlace)
                }

                override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                    super.onAdFailedToPreload(preloadId, adError)
                    DataLogger.e(
                        "Rewarded ${adHolder.adPlace.placeName} onAdFailedToPreload " +
                                "code=${adError.code} msg=${adError.message}"
                    )
                    adHolder.isLoading = false
                    notifyAdFullScreenNotValidOrLoadFailed(adHolder.adPlace)
                }

                override fun onAdsExhausted(preloadId: String) {
                    // Buffer rỗng (vừa pollAd xong) — SDK sẽ tự nạp tiếp theo bufferSize.
                    DataLogger.d("Rewarded ${adHolder.adPlace.placeName} onAdsExhausted id=$preloadId")
                    adHolder.hasNotifiedThisBatch = false
                }

            }
        )
        if (!started) {
            // start() trả false khi adUnitId đã được start trước đó với config khác/đang chạy.
            DataLogger.d("preloadRewarded ${adHolder.adPlace.placeName} start() returned false")
            adHolder.isLoading = false
        }
    }

    private fun loadBannerNativeAd(placeName: String, subPlaceLevel: Int, isCanShowBannerCollapsibleIfNeed: Boolean) {
        val adPlace = getAdPlaceBy(placeName)

        if (!adPlace.isBannerOrNativePlace()) {
            return
        }

        if (isNotAbleToVisibleAdsToUser(placeName)) {
            notifyBannerNativeFailedToLoad(placeName, subPlaceLevel)
            return
        }

        val adHolder = getOrCreateBannerNativeAdHolderBy(adPlace, subPlaceLevel)
        adHolder.needRetry = true
        if (!adHolder.isNeedNotifyEvent) {
            adHolder.isNeedNotifyEvent = true
        }
        adHolder.subPlaceLevel = subPlaceLevel

        if (adHolder is BannerAdHolder) {
            adHolder.isCanShowBannerCollapsibleIfNeed = isCanShowBannerCollapsibleIfNeed
        }

        if (adPlace.isNativeType()) {
            val nativeHolder = adHolder as NativeAdHolder
            if (nativeHolder.isAdLoaded() && nativeHolder.nativeAd != null) {
                notifyNativeLoaded(nativeHolder)
                return
            }
            loadNativeAdIfNeed(adHolder)
            return
        }

        if (adPlace.isBannerType()) {
            val bannerHolder = adHolder as BannerAdHolder
            if (bannerHolder.isAdLoaded() && bannerHolder.bannerAd != null) {
                notifyBannerLoaded(null, adPlace as BannerAdPlace, subPlaceLevel)
                return
            }
            val activity = currentActivity ?: context.findActivity()
            loadBannerAdIfNeed(activity, bannerHolder)
            return
        }
    }

    private fun notifyBannerNativeFailedToLoad(placeName: String, subPlaceLevel: Int) {
        applicationScope.launch {
            _adLoadBannerNativeFlow.emit(
                AdLoadBannerNativeUiResource.AdFailed(
                    placeName,
                    subPlaceLevel
                )
            )
        }
    }

    private fun notifyBannerLoading(bannerAdPlace: BannerAdPlace, subPlaceLevel: Int) {
        applicationScope.launch {
            _adLoadBannerNativeFlow.emit(
                AdLoadBannerNativeUiResource.Loading.LoadingBanner(
                    bannerAdPlace,
                    subPlaceLevel,
                )
            )
        }
    }

    private fun notifyBannerLoaded(
        bannerAd: ViewGroup?,
        bannerAdPlace: BannerAdPlace,
        subPlaceLevel: Int
    ) {
        bannerAd?.let {
            applicationScope.launch {
                _adLoadBannerNativeFlow.emit(
                    AdLoadBannerNativeUiResource.BannerAdLoaded(
                        bannerAd = bannerAd,
                        bannerAdPlace = bannerAdPlace,
                        subPlaceLevel = subPlaceLevel
                    )
                )
            }
        }
    }

    private fun notifyNativeLoaded(nativeAdHolder: NativeAdHolder) {
        applicationScope.launch {
            nativeAdHolder.nativeAd?.let {
                _adLoadBannerNativeFlow.emit(
                    AdLoadBannerNativeUiResource.NativeAdLoaded(
                        it,
                        nativeAdHolder.adPlace as NativeAdPlace,
                        nativeAdHolder.subPlaceLevel
                    )
                )
            }
        }
    }

    private fun loadNativeAdIfNeed(
        adHolder: NativeAdHolder
    ) {
        if (adHolder.isLoading) {
            DataLogger.d("loadNativeAdIfNeed skipped: already loading ${adHolder.adPlace.placeName}")
            return
        }
        val adPlace = getAdPlaceBy(adHolder.adPlace.placeName)

        applicationScope.launch(Dispatchers.Main) {
            val videoOptions = VideoOptions.Builder()
                .setStartMuted(true)
                .setCustomControlsRequested(false)
                .setClickToExpandRequested(false)
                .build()

            val adRequest = NativeAdRequest
                .Builder(getAdUnit(adPlace).adUnitId, listOf(NativeAd.NativeAdType.NATIVE))
                .setMediaAspectRatio(NativeAd.NativeMediaAspectRatio.ANY)
                .setVideoOptions(videoOptions)
                .setAdChoicesPlacement(AdChoicesPlacement.TOP_RIGHT)
                .build()

            val adCallback = object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    super.onNativeAdLoaded(nativeAd)
                    DataLogger.d("loadNativeAdIfNeed onNativeAdLoaded: PlaceName:${adHolder.adPlace.placeName}")
                    adHolder.nativeAd = nativeAd
                    adHolder.loadedTimeInSecond = getCurrentTimeInSecond()
                    adHolder.isLoading = false

                    nativeAd.adEventCallback = object : NativeAdEventCallback {
                        override fun onAdImpression() {
                            super.onAdImpression()
                            DataLogger.d("loadNativeAdIfNeed Ad impression")
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            DataLogger.e("loadNativeAdIfNeed onAdClicked")
                        }
                    }
                    notifyNativeLoaded(adHolder)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    DataLogger.e("loadNativeAdIfNeed onAdFailedToLoad: ${adError.message}")
                    adHolder.isLoading = false
                    notifyBannerNativeFailedToLoad(adHolder.adPlace.placeName, adHolder.subPlaceLevel)
                }
            }
            adHolder.isLoading = true
            NativeAdLoader.load(adRequest, adCallback)
        }
    }

    private fun loadBannerAdIfNeed(
        activity: Activity?,
        adHolder: BannerAdHolder,
    ) {
        val bannerAdPlace = adHolder.adPlace as BannerAdPlace

        // MỚI: Ưu tiên dùng Activity context để khởi tạo View và tính toán AdSize
        val adContext = activity ?: context

        val screenHeightDp =
            (activity?.getScreenHeightDp() ?: context.getScreenHeightDpByContext()).toFloat()
        val screenWidthDp =
            (activity?.getScreenWidthDp() ?: context.getScreenWidthDpByContext()).toFloat()

        // MỚI: Đưa logic xác định size ra ngoài, không bọc toàn bộ hàm bằng if (isAnchored) nữa
        if (bannerAdPlace.bannerSize == BannerSize.LargeAnchored) {
            val ratio = screenHeightDp / screenWidthDp
            val isTallScreen = ratio >= 1.75f
            val hasEnoughHeight = screenHeightDp >= 720
            val useLargeAnchored = isTallScreen && hasEnoughHeight
            bannerAdPlace.bannerSize =
                if (useLargeAnchored) BannerSize.LargeAnchored else BannerSize.Anchored
        }

        val rawScreenWidthDp = activity?.getBannerAdWidth() ?: context.getBannerAdWidthByContext()

        // MỚI: Sửa lại công thức tính max width dựa trên screenWidth thay vì screenHeight
        val maxBannerWidthDp = (screenWidthDp * 0.96f).toInt().coerceAtLeast(320)
        val bannerAdWidth = minOf(rawScreenWidthDp, maxBannerWidthDp)

        val adSize = when (bannerAdPlace.bannerSize) {
            BannerSize.LargeAnchored -> AdSize.getLargeAnchoredAdaptiveBannerAdSize(
                adContext,
                bannerAdWidth
            )

            BannerSize.Anchored -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                adContext,
                bannerAdWidth
            )

            BannerSize.Inline -> {
                if (bannerAdPlace.bannerInlineMaxHeight > 32) {
                    AdSize.getInlineAdaptiveBannerAdSize(
                        rawScreenWidthDp,
                        bannerAdPlace.bannerInlineMaxHeight
                    )
                } else {
                    AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(
                        adContext,
                        rawScreenWidthDp
                    )
                }
            }

            BannerSize.StandardMedium -> AdSize.MEDIUM_RECTANGLE
            BannerSize.StandardLarge -> AdSize.LARGE_BANNER
        }

        try {
            adHolder.bannerAd?.let { stale ->
                (stale.parent as? ViewGroup)?.let { staleParent ->
                    staleParent.endViewTransition(stale)
                    staleParent.layoutTransition = null
                    staleParent.removeView(stale)
                }
                stale.destroy()
            }

            adHolder.bannerAd = null
            adHolder.hasNotifiedThisBatch = false

            val bannerAd = AdView(adContext)
            bannerAd.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            val adUnitId = getAdUnit(adHolder.adPlace).adUnitId

            val bannerAdRequestBuilder = BannerAdRequest.Builder(adUnitId, adSize)
            val collapsiblePosition = bannerAdPlace.bannerCollapsiblePosition
            if (!collapsiblePosition.isNullOrBlank() && adHolder.isCanShowBannerCollapsibleIfNeed) {
                if (!bannerAdPlace.bannerCollapsibleIsShowOnlyOnce || adHolder.collapsibleShowCount == 0) {
                    val extras = Bundle().apply {
                        putString("collapsible", collapsiblePosition)
                    }
                    bannerAdRequestBuilder.setGoogleExtrasBundle(extras)
                    DataLogger.d("${adHolder.getTag()} ${bannerAdPlace.placeName} requesting collapsible banner at position=$collapsiblePosition")
                }
            }
            val adRequest = bannerAdRequestBuilder.build()

            val adLoadCallback = object : AdLoadCallback<BannerAd> {
                private var destroyed = false
                override fun onAdLoaded(ad: BannerAd) {
                    super.onAdLoaded(ad)
                    if (destroyed || adHolder.bannerAd !== bannerAd) return
                    DataLogger.d("loadBannerAdIfNeed: Banner Ad Loaded")

                    val placeName = adHolder.adPlace.placeName
                    adHolder.retryCount = 0

                    ad.bannerAdRefreshCallback = object : BannerAdRefreshCallback {
                        override fun onAdRefreshed() {
                            super.onAdRefreshed()
                            DataLogger.d("${adHolder.getTag()} $placeName auto-refreshed by SDK")
                            adHolder.adNetworkName =
                                getUniqueAdSourceName(ad.getResponseInfo().loadedAdSourceResponseInfo)
                        }

                        override fun onAdFailedToRefresh(adError: LoadAdError) {
                            super.onAdFailedToRefresh(adError)
                            if (adError.message.contains("CANCELLED", ignoreCase = true) ||
                                adError.message.contains(
                                    "JobCancellationException",
                                    ignoreCase = true
                                )
                            ) {
                                DataLogger.d("${adHolder.getTag()} $placeName auto-refresh cancelled: ${adError.message}")
                            } else {
                                DataLogger.e("${adHolder.getTag()} $placeName auto-refresh failed (code=${adError.code}, msg=${adError.message})")
                            }
                        }
                    }

                    ad.adEventCallback = object : BannerAdEventCallback {
                        override fun onAdPaid(value: AdValue) {
                            super.onAdPaid(value)
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                        }

                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            adHolder.collapsibleShowCount++
                            if (bannerAdPlace.bannerCollapsibleIsShowOnlyOnce) {
                                adHolder.isCanShowBannerCollapsibleIfNeed = false
                            }
                            DataLogger.d("${adHolder.getTag()} $placeName banner collapsible open (count=${adHolder.collapsibleShowCount})")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()
                            DataLogger.d("${adHolder.getTag()} $placeName banner collapsible closed")
                        }
                    }

                    adHolder.loadedTimeInSecond = getCurrentTimeInSecond()
                    adHolder.adNetworkName =
                        getUniqueAdSourceName(ad.getResponseInfo().loadedAdSourceResponseInfo)
                    adHolder.isLoading = false
                    adHolder.isLoadSuccess = true

                    notifyBannerLoaded(bannerAd, bannerAdPlace, adHolder.subPlaceLevel)
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    super.onAdFailedToLoad(adError)
                    if (destroyed || adHolder.bannerAd !== bannerAd) {
                        DataLogger.d("${adHolder.getTag()} $bannerAdPlace late onAdFailedToLoad ignored. code=${adError.code}")
                        return
                    }

                    adHolder.retryCount += 1
                    val maxRetryCount = remoteConfigRepository.getConfigAdsBanner().maxRetryCount
                    if (shouldStopBannerRetry(adHolder.retryCount, maxRetryCount)) {
                        DataLogger.d("Banner Ad reached maxRetryCount=${maxRetryCount}")
                        destroyed = true
                        notifyBannerNativeFailedToLoad(
                            bannerAdPlace.placeName,
                            adHolder.subPlaceLevel
                        )
                    }
                }
            }

            bannerAd.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS
            bannerAd.loadAd(adRequest, adLoadCallback)
            adHolder.bannerAd = bannerAd

        } catch (e: Exception) {
            DataLogger.e("loadBannerAdIfNeed Exception: ${e.message}")
            notifyBannerNativeFailedToLoad(adHolder.adPlace.placeName, adHolder.subPlaceLevel)
        }
    }

    private fun getOrCreateFullscreenAdHolderBy(
        adPlace: AdPlace
    ): AdHolder {
        val adIdAllPrice =
            adPlace.adUnit.adUnitId
        var adHolder = adHolderFullScreenMap[adIdAllPrice]
        if (adHolder == null || adHolder.adPlace.adType != adPlace.adType) {
            adHolder = when (adPlace.adType) {
                AdType.Interstitial -> InterstitialAdHolder(adPlace = adPlace)
                AdType.RewardedInterstitial -> RewardedInterstitialAdHolder(adPlace = adPlace)
                AdType.Rewarded -> RewardedAdHolder(adPlace)
                else -> InterstitialAdHolder(adPlace)
            }
            adHolderFullScreenMap[adIdAllPrice] = adHolder
        }
        adHolder.adPlace = adPlace
        return adHolder
    }

    private fun getOrCreateBannerNativeAdHolderBy(
        adPlace: AdPlace,
        subPlaceModel: Int = 0,
        orientationOverride: Int? = null
    ): AdHolder {
        val key = getAdHolderMapKey(adPlace, subPlaceModel, orientationOverride)
        var adHolder = adHolderMap[key]
        if (adHolder == null || adHolder.adPlace.adType != adPlace.adType) {
            adHolder = when (adPlace.adType) {
                AdType.Banner -> {
                    try {
                        val reuseHolder =
                            adHolderMap.values.filter {
                                it is BannerAdHolder
                                        && !it.isShowed
                                        && (it.bannerAd != null)
                            }
                                .firstOrNull { it.adPlace.isCanReuseAds }
                        if (reuseHolder != null && !adPlace.isCanReuseAds) {
                            reuseHolder
                        } else {
                            BannerAdHolder(adPlace = adPlace)
                        }
                    } catch (_: Exception) {
                        BannerAdHolder(adPlace = adPlace)
                    }
                }

                else -> {
                    try {
                        val reuseHolder =
                            adHolderMap.values.filter { it is NativeAdHolder && !it.isShowed && it.nativeAd != null }
                                .firstOrNull {
                                    (it.adPlace as NativeAdPlace).isSameTemplate(adPlace) && it.adPlace.isCanReuseAds
                                }
                        if (reuseHolder != null && !adPlace.isCanReuseAds) {
                            DataLogger.d("NativeAd ${adPlace.placeName} holder reused from ${reuseHolder.adPlace.placeName}")
                            reuseHolder
                        } else {
                            DataLogger.d("NativeAd ${adPlace.placeName} holder fresh (no reusable candidate)")
                            NativeAdHolder(adPlace = adPlace)
                        }
                    } catch (_: Exception) {
                        NativeAdHolder(adPlace = adPlace)
                    }
                }
            }
            adHolder.subPlaceLevel = subPlaceModel
            adHolderMap[key] = adHolder
        } else {
            when (adHolder) {
                is BannerAdHolder -> {
                    if (adHolder.bannerAd == null) {
                        val reuseHolder =
                            adHolderMap.values.filter {
                                it is BannerAdHolder
                                        && !it.isShowed
                                        && (it.bannerAd != null)
                            }
                                .firstOrNull { it.adPlace.isCanReuseAds }
                        if (reuseHolder != null && reuseHolder is BannerAdHolder && !adPlace.isCanReuseAds) {
                            DataLogger.d("BannerAd ${adPlace.placeName} ad reused from ${reuseHolder.adPlace.placeName}")
                            adHolder.bannerAd = reuseHolder.bannerAd
                            reuseHolder.bannerAd = null
                        }
                    }
                }

                is NativeAdHolder -> {
                    if (adHolder.nativeAd == null) {
                        val reuseHolder =
                            adHolderMap.values.filter { it is NativeAdHolder && !it.isShowed && it.nativeAd != null }
                                .firstOrNull {
                                    (it.adPlace as NativeAdPlace).isSameTemplate(adPlace) && it.adPlace.isCanReuseAds
                                }
                        if (reuseHolder != null && reuseHolder is NativeAdHolder && !adPlace.isCanReuseAds) {
                            DataLogger.d("NativeAd ${adPlace.placeName} ad reused from ${reuseHolder.adPlace.placeName}")
                            adHolder.nativeAd = reuseHolder.nativeAd
                            reuseHolder.nativeAd = null
                        }
                    }
                }

                else -> {}
            }
        }
        adHolder.adPlace = adPlace
        return adHolder
    }

    private fun getAdHolderMapKey(
        adPlace: AdPlace,
        subPlaceModel: Int,
        orientationOverride: Int? = null,
    ): String {
        val placeKey = adPlace.placeName.ifBlank { adPlace.adUnit.adUnitId }
        var subKeyPlaceScreenLevelInOrder = ""
        if (adPlace.placeScreenLevelInOrder != null) {
            subKeyPlaceScreenLevelInOrder = "-${adPlace.placeScreenLevelInOrder}"
        }
        var collapsiblePosition = ""
        if (adPlace is BannerAdPlace && !adPlace.bannerCollapsiblePosition.isNullOrBlank()) {
            collapsiblePosition = adPlace.getKeyCollapsible()
        }
        var nativeFull = ""
        if (adPlace is NativeAdPlace && adPlace.nativeTemplateType == NativeTemplateType.Full) {
            nativeFull = "nvf;" // nvf -> native_full
        }
        var orientationKey = ""
        if (adPlace is BannerAdPlace) {
            val orientation = orientationOverride
                ?: this
            orientationKey = "o$orientation;"   // "o1;" portrait, "o2;" landscape
        }
        return "$placeKey;${adPlace.placeScreenLevel};$subKeyPlaceScreenLevelInOrder;$nativeFull$subPlaceModel;$collapsiblePosition$orientationKey"
    }

    private fun shouldStopBannerRetry(retryCount: Int, maxRetryCount: Int): Boolean = when {
        maxRetryCount < 0 -> false            // unlimited — never stop
        maxRetryCount == 0 -> true            // immediate stop on first failure
        else -> retryCount >= maxRetryCount   // stop when reached limit
    }

    private fun isNotAbleToVisibleAdsToUserRewardedAndRewardedInterstitial(
        placeName: String
    ): Boolean {
        if (isCountryNotAvailableToShowAd()) {
            DataLogger.d("Visibility(rewarded) $placeName blocked: country not allowed")
            return true
        }
        return false
    }

    private fun showRewardedInterstitialVideo(
        activity: Activity,
        adHolder: RewardedInterstitialAdHolder,
    ) {
        val adPlace = adHolder.adPlace
        val placeName = adPlace.placeName

        // Huỷ phiên chờ-load trước đó (nếu user bấm lại) để không show 2 ad chồng nhau.
        fullScreenAdWaitLoadJob?.cancel()
        fullScreenAdWaitLoadJob = null

        if (adHolder.isShowing) {
            DataLogger.d("← showRewardedInterstitialVideo $placeName skipped: already showing")
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            DataLogger.e("← showRewardedInterstitialVideo $placeName skipped: activity invalid")
            notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.Other)
            return
        }

        // Ưu tiên ad đã giữ sẵn ở holder (từ fallback load), nếu không có thì poll từ buffer.
        val rewardedAd = adHolder.rewardedInterstitialAd ?: pollRewardedInterstitialAd(adPlace)

        if (rewardedAd == null) {
            DataLogger.d("showRewardedInterstitialVideo $placeName: no ad in buffer")
            handleRewardedInterstitialAdUnavailable(activity, adHolder)
            return
        }
        showRewardedInterstitialAdInternal(activity, adHolder, rewardedAd)
    }

    /**
     * Show rewarded ad theo next-gen SDK:
     * ad nằm trong buffer của [RewardedAdPreloader], phải `pollAd()` ngay lúc show
     * (preload KHÔNG trả ad về `adHolder.rewarded`).
     */
    private fun showRewardedVideo(
        activity: Activity,
        adHolder: RewardedAdHolder,
    ) {
        val adPlace = adHolder.adPlace
        val placeName = adPlace.placeName

        // Huỷ phiên chờ-load trước đó (nếu user bấm lại) để không show 2 ad chồng nhau.
        fullScreenAdWaitLoadJob?.cancel()
        fullScreenAdWaitLoadJob = null

        if (adHolder.isShowing) {
            DataLogger.d("← showRewardedVideo $placeName skipped: already showing")
            return
        }
        if (activity.isFinishing || activity.isDestroyed) {
            DataLogger.e("← showRewardedVideo $placeName skipped: activity invalid")
            notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.Other)
            return
        }

        // Ưu tiên ad đã giữ sẵn ở holder (từ fallback load), nếu không có thì poll từ buffer.
        val rewardedAd = adHolder.rewarded ?: pollRewardedAd(adPlace)

        if (rewardedAd == null) {
            DataLogger.d("showRewardedVideo $placeName: no ad in buffer")
            handleRewardedAdUnavailable(activity, adHolder)
            return
        }
        showRewardedAdInternal(activity, adHolder, rewardedAd)
    }

    /** Lấy 1 ad ra khỏi buffer của [RewardedAdPreloader]. Ad đã poll chỉ show được 1 lần. */
    private fun pollRewardedAd(adPlace: AdPlace): RewardedAd? {
        val adUnitId = getAdUnit(adPlace).adUnitId
        if (adUnitId.isBlank()) return null
        return try {
            RewardedAdPreloader.pollAd(adUnitId).also {
                DataLogger.d(
                    "pollRewardedAd ${adPlace.placeName} ad=${it != null} " +
                            "remaining=${RewardedAdPreloader.getNumAdsAvailable(adUnitId)}"
                )
            }
        } catch (e: Exception) {
            DataLogger.e("pollRewardedAd ${adPlace.placeName} error: ${e.message}")
            null
        }
    }

    /** Gắn callback + show. Luôn chạy trên main thread theo yêu cầu của SDK. */
    private fun showRewardedAdInternal(
        activity: Activity,
        adHolder: RewardedAdHolder,
        rewardedAd: RewardedAd
    ) {
        val adPlace = adHolder.adPlace
        adHolder.rewarded = rewardedAd
        adHolder.isEarnedReward = false
        adHolder.amount = 0
        adHolder.isShowing = true
        adHolder.isWaitLoadToShow = false

        rewardedAd.setImmersiveMode(true)
        rewardedAd.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
                DataLogger.d("Rewarded ${adPlace.placeName} showed")
                adHolder.isShowed = true
                notifyAdFullScreenSucceedToShow(adPlace)
            }

            override fun onAdFailedToShowFullScreenContent(
                fullScreenContentError: FullScreenContentError
            ) {
                super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                DataLogger.e(
                    "Rewarded ${adPlace.placeName} failed to show " +
                            "code=${fullScreenContentError.code} msg=${fullScreenContentError.message}"
                )
                // Ad hỏng → bỏ hẳn, nạp lại buffer, và báo UI (cho phép retry).
                finishRewardedSession(adHolder)
                notifyAdFullScreenFailedToShow(adPlace, true)
                notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.Other)
            }

            override fun onAdImpression() {
                super.onAdImpression()
                DataLogger.d("Rewarded ${adPlace.placeName} impression")
            }

            override fun onAdClicked() {
                super.onAdClicked()
                DataLogger.d("Rewarded ${adPlace.placeName} clicked")
            }

            override fun onAdPaid(value: AdValue) {
                super.onAdPaid(value)
                DataLogger.d(
                    "Rewarded ${adPlace.placeName} paid " +
                            "${value.valueMicros} ${value.currencyCode}"
                )
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                // Chốt kết quả reward đúng 1 lần, tại thời điểm đóng ad.
                val isEarnedReward = adHolder.isEarnedReward
                val amount = adHolder.amount
                DataLogger.d(
                    "Rewarded ${adPlace.placeName} dismissed earned=$isEarnedReward amount=$amount"
                )
                finishRewardedSession(adHolder)
                notifyAdFullScreenCompleted(
                    adPlace = adPlace,
                    isEarnedReward = isEarnedReward,
                    amount = amount,
                    adErrorType = AdErrorType.None
                )
            }
        }

        activity.runOnUiThread {
            if (activity.isFinishing || activity.isDestroyed) {
                DataLogger.e("Rewarded ${adPlace.placeName} abort show: activity gone")
                finishRewardedSession(adHolder)
                notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.Other)
                return@runOnUiThread
            }
            rewardedAd.show(activity) { rewardItem ->
                // Chỉ ghi nhận, KHÔNG notify ở đây — chờ onAdDismissed để tránh
                // UI mở màn reward trong khi ad vẫn đang hiển thị.
                adHolder.isEarnedReward = true
                adHolder.amount = rewardItem.amount
                DataLogger.d(
                    "Rewarded ${adPlace.placeName} earned ${rewardItem.amount} ${rewardItem.type}"
                )
            }
        }
    }

    /** Dọn state sau mỗi lần show (thành công hay thất bại) + nạp lại buffer cho lần sau. */
    private fun finishRewardedSession(adHolder: RewardedAdHolder) {
        adHolder.rewarded?.adEventCallback = null
        adHolder.rewarded = null
        adHolder.isShowing = false
        adHolder.isWaitLoadToShow = false
        adHolder.hasNotifiedThisBatch = false
        fullScreenAdWaitLoadJob?.cancel()
        fullScreenAdWaitLoadJob = null
        // Nạp lại ngay để lần show kế tiếp có ad sẵn trong buffer.
        preloadRewarded(adHolder)
    }

    /**
     * Không có ad lúc show:
     *  - isWaitLoadToShow = true  → chờ preloader nạp xong (có timeout) rồi show.
     *  - isWaitLoadToShow = false → báo completed ngay (không reward) và preload cho lần sau.
     */
    private fun handleRewardedAdUnavailable(
        activity: Activity,
        adHolder: RewardedAdHolder,
    ) {
        val adPlace = adHolder.adPlace
        val adUnitId = getAdUnit(adPlace).adUnitId

        if (!adHolder.isWaitLoadToShow) {
            DataLogger.d("showRewardedVideo ${adPlace.placeName}: no ad, not waiting → complete")
            preloadRewarded(adHolder)
            notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.NoFill)
            return
        }

        // Đảm bảo preloader đang chạy cho ad unit này.
        preloadRewarded(adHolder)

        fullScreenAdWaitLoadJob = applicationScope.launch {
            val maxAttempt = WAIT_LOAD_TIMEOUT_MS / FULL_SCREEN_AD_POLL_INTERVAL_MS
            var attempt = 0L
            var polledAd: RewardedAd? = null

            while (isActive && attempt < maxAttempt) {
                if (activity.isFinishing || activity.isDestroyed) break
                if (RewardedAdPreloader.isAdAvailable(adUnitId)) {
                    polledAd = pollRewardedAd(adPlace)
                    if (polledAd != null) break
                }
                delay(FULL_SCREEN_AD_POLL_INTERVAL_MS)
                attempt++
            }

            withContext(Dispatchers.Main) {
                val ad = polledAd
                when {
                    ad == null -> {
                        DataLogger.e("showRewardedVideo ${adPlace.placeName}: wait-load timeout")
                        adHolder.isWaitLoadToShow = false
                        notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.NoFill)
                    }

                    activity.isFinishing || activity.isDestroyed -> {
                        DataLogger.e("showRewardedVideo ${adPlace.placeName}: activity gone after wait")
                        adHolder.isWaitLoadToShow = false
                        notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.Other)
                    }

                    else -> showRewardedAdInternal(activity, adHolder, ad)
                }
            }
        }
    }

    private fun handleRewardedInterstitialAdUnavailable(
        activity: Activity,
        adHolder: RewardedInterstitialAdHolder,
    ) {
        val adPlace = adHolder.adPlace
        val adUnitId = getAdUnit(adPlace).adUnitId

        if (!adHolder.isWaitLoadToShow) {
            DataLogger.d("showRewardedVideo ${adPlace.placeName}: no ad, not waiting → complete")
            preloadRewardedInterstitialNew(adHolder)
            notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.NoFill)
            return
        }

        // Đảm bảo preloader đang chạy cho ad unit này.
        preloadRewardedInterstitialNew(adHolder)

        fullScreenAdWaitLoadJob = applicationScope.launch {
            val maxAttempt = WAIT_LOAD_TIMEOUT_MS / FULL_SCREEN_AD_POLL_INTERVAL_MS
            var attempt = 0L
            var polledAd: RewardedInterstitialAd? = null

            while (isActive && attempt < maxAttempt) {
                if (activity.isFinishing || activity.isDestroyed) break
                if (RewardedAdPreloader.isAdAvailable(adUnitId)) {
                    polledAd = pollRewardedInterstitialAd(adPlace)
                    if (polledAd != null) break
                }
                delay(FULL_SCREEN_AD_POLL_INTERVAL_MS)
                attempt++
            }

            withContext(Dispatchers.Main) {
                val ad = polledAd
                when {
                    ad == null -> {
                        DataLogger.e("showRewardedVideo ${adPlace.placeName}: wait-load timeout")
                        adHolder.isWaitLoadToShow = false
                        notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.NoFill)
                    }

                    activity.isFinishing || activity.isDestroyed -> {
                        DataLogger.e("showRewardedVideo ${adPlace.placeName}: activity gone after wait")
                        adHolder.isWaitLoadToShow = false
                        notifyAdFullScreenCompleted(adPlace, false, 0, AdErrorType.Other)
                    }

                    else -> showRewardedInterstitialAdInternal(activity, adHolder, ad)
                }
            }
        }
    }

    /** Dọn state sau mỗi lần show (thành công hay thất bại) + nạp lại buffer cho lần sau. */
    private fun finishRewardedInterstitialSession(adHolder: RewardedInterstitialAdHolder) {
        adHolder.rewardedInterstitialAd?.adEventCallback = null
        adHolder.rewardedInterstitialAd = null
        adHolder.isShowing = false
        adHolder.isWaitLoadToShow = false
        adHolder.hasNotifiedThisBatch = false
        fullScreenAdWaitLoadJob?.cancel()
        fullScreenAdWaitLoadJob = null
        // Nạp lại ngay để lần show kế tiếp có ad sẵn trong buffer.
        preloadRewardedInterstitialNew(adHolder)
    }

    private fun pollRewardedInterstitialAd(adPlace: AdPlace): RewardedInterstitialAd? {
        val adUnitId = getAdUnit(adPlace).adUnitId
        if (adUnitId.isBlank()) return null
        return try {
            RewardedInterstitialAdPreloader.pollAd(adUnitId).also {
                DataLogger.d(
                    "pollRewardedAd ${adPlace.placeName} ad=${it != null} " +
                            "remaining=${RewardedAdPreloader.getNumAdsAvailable(adUnitId)}"
                )
            }
        } catch (e: Exception) {
            DataLogger.e("pollRewardedAd ${adPlace.placeName} error: ${e.message}")
            null
        }
    }
}