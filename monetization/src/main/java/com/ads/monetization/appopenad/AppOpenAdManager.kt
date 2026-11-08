package com.ads.monetization.appopenad

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.androidtech.data.logger.DataLogger
import com.androidtech.data.model.configads.PreventShowManyInterstitialAds
import com.androidtech.data.network.manager.isNetworkConnected
import com.androidtech.data.remoteconfig.RemoteConfigService
import com.androidtech.data.utils.getCurrentTimeInSecond
import com.androidtech.data.utils.toMillis
import com.androidtech.domain.model.configad.AdPlace
import com.androidtech.domain.model.configad.AppOpenAdPlace
import com.androidtech.domain.repository.RemoteConfigRepository
import com.ads.monetization.AdManager
import com.ads.monetization.adholder.AppOpenAdHolder
import com.ads.monetization.adplacename.AdPlaceName
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class AppOpenAdManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val adManager: AdManager,
    private val remoteConfigService: RemoteConfigService
) : Application.ActivityLifecycleCallbacks {
    companion object {
        const val TAG = "AdmobManager"
        private const val WAIT_LOAD_TIMEOUT_MS = 15_000L
        var skipAppReopenAds = false
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)


    private val _adOpenAppFlow = MutableSharedFlow<AdOpenAdUiResource>()
    val adOpenAppFlow = _adOpenAppFlow.asSharedFlow()

    private val adHolderMap = mutableMapOf<String, AppOpenAdHolder>()

    /** Watchdog timeout jobs cho AppOpen `isWaitLoadToShow` flow keyed by placeName.
     *  Schedule khi fetchAd với isWaitLoadToShow=true, cancel khi load success/fail
     *  hoặc show success/fail. Timeout 15s — nếu ad không load kịp, give up. */
    private val waitLoadTimeoutJobs = mutableMapOf<String, Job>()

    private var currentActivity: Activity? = null

    private var isShowingAd = false

    var isSessionStart = true

    var isIgnoreSplashToForegroundWhenAppOpen = true

    var isAppOpenAdDismissedDelivered = false

    var adPlaceNotDismissedDelivered = ""

    var needShowAdAgainDueIgnoreShowWhenAppGoToBackground = false
    var needShowAdAgainDueIgnoreShowWhenAppGoToBackgroundAdPlaceName = ""

    init {
        val app = (context as Application)
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object :DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                super.onStart(owner)
                if (needShowAdAgainDueIgnoreShowWhenAppGoToBackground) {
                    needShowAdAgainDueIgnoreShowWhenAppGoToBackground = false
                    val placeName = needShowAdAgainDueIgnoreShowWhenAppGoToBackgroundAdPlaceName
                    needShowAdAgainDueIgnoreShowWhenAppGoToBackgroundAdPlaceName = ""
                    currentActivity?.let { activity ->
                        showAdIfAvailable(activity, placeName)
                    }
                    return
                }

                if (isSessionStart) {
                    return
                }
                currentActivity?.let { activity ->
                    showAdIfAvailable(activity, AdPlaceName.OPEN_APP)
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                super.onStop(owner)
                if (isSessionStart) {
                    isSessionStart = false
                    return
                }

                if (isShowingAd) {
                    return
                }
                currentActivity?.let {
                    fetchAd(it, AdPlaceName.OPEN_APP)
                }
            }
        })
    }

    fun setupDefaultValue() {
        isSessionStart = true
        isShowingAd = false
        isAppOpenAdDismissedDelivered = true
        adPlaceNotDismissedDelivered = ""
    }

    fun fetchAd(activity: Activity, adPlaceName: String) {
        if (!MobileAds.isInitialized) {
            notifyAdNotValidOrLoadFailed(adPlaceName)
            return
        }
        if (!remoteConfigService.getRemoteConfigAdsActive()) {
            DataLogger.d("fetchAppOpenAd: AdActive = false")
            notifyAdNotValidOrLoadFailed(adPlaceName)
            return
        }
        if (adManager.isNotAbleToVisibleAdsToUser(adPlaceName)) {
            notifyAdNotValidOrLoadFailed(adPlaceName)
            return
        }
        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)
        val adHolder = getOrCreateAdHolderBy(adPlace)

        if (adHolder.isLoading) {
            return
        }
        if (adHolder.isAdAvailable()) {
            notifyAdOpenAppLoaded(adPlaceName)
            return
        }
        adHolder.isLoading = true

        // Schedule timeout watchdog cho wait-load flow. Nếu sau 15s vẫn chưa
        // load xong, giả sử mạng/SDK bị kẹt → notify failed + reset.
        if (adHolder.isWaitLoadToShow) {
            scheduleWaitLoadTimeout(adPlaceName, adHolder)
        }

        val timeStartLoad = System.currentTimeMillis()
        DataLogger.d( "AppOpenAd $adPlaceName start load")
        val adLoadCallback = object : AdLoadCallback<AppOpenAd> {
            override fun onAdLoaded(ad: AppOpenAd) {
                // Called when an ad has loaded.
                DataLogger.d( "AppOpenAd $adPlaceName loaded; ${System.currentTimeMillis() - timeStartLoad}")
                adHolder.isLoading = false
                adHolder.adNetworkName = adManager.getUniqueAdSourceName(ad.getResponseInfo().loadedAdSourceResponseInfo)
                adHolder.appOpenAd = ad
                // Load thành công → cancel timeout watchdog.
                cancelWaitLoadTimeout(adPlaceName)
                notifyAdOpenAppLoaded(adPlaceName)
                startCountDownTimerIfNeed()
                if (adHolder.isWaitLoadToShow) {
                    showAdIfAvailable(activity, adPlaceName)
                    adHolder.isWaitLoadToShow = false
                }
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                DataLogger.d( "AppOpenAd $adPlaceName load failed; with code: ${adError.code}; ${adError.message}")
//                Firebase.crashlytics.recordException(Throwable("${adError.code}; ${adError.message}"))
                adHolder.isLoading = false
                // Load failed → cancel timeout watchdog (won't fire timeout sau).
                cancelWaitLoadTimeout(adPlaceName)
                if (activity.isNetworkConnected()) {
//                    adManager.disableLoadAdsWhenLoadFailed(adHolder)
                }

                val appOpenAdConfig = remoteConfigRepository.getConfigAdsAppOpen()
                val previousId = adHolder.adPlace.adUnit.adUnitId
                val isTheSameUnitId = adManager.getAdUnit(adHolder.adPlace).adUnitId == previousId

                val maxRetryCount = appOpenAdConfig.maxRetryCount
                val retryIntervalSecondList = if (isTheSameUnitId) {
                    appOpenAdConfig.delayTimesSameUnitId
                } else {
                    appOpenAdConfig.delayTimesDifferentUnitId
                }
                when {
                    (adHolder.retryCount in 0 until maxRetryCount) && retryIntervalSecondList.isNotEmpty() -> {
                        adHolder.retryCount++
                        val currentRetryCount = adHolder.retryCount
                        val retryDelay = if (currentRetryCount >= retryIntervalSecondList.size) {
                            retryIntervalSecondList[retryIntervalSecondList.size - 1]
                        } else {
                            retryIntervalSecondList[currentRetryCount - 1]
                        }

                        DataLogger.d( "AppOpenAd $adPlaceName retry load ${adHolder.retryCount}")
                        applicationScope.launch {
                            withContext(Dispatchers.IO) {
                                delay(retryDelay.toMillis())
                            }
                            fetchAd(activity, adPlaceName)
                        }
                    }

                    else -> {
                        DataLogger.d( "AppOpenAd $adPlaceName load failed")
                        notifyAdNotValidOrLoadFailed(adPlaceName)
                        adHolder.reset()
                    }
                }
            }
        }
        AppOpenAd.load(
            adManager.getAdRequest(adManager.getAdUnit(adHolder.adPlace).adUnitId),
            adLoadCallback
        )
    }

    fun showAdIfAvailable(activity: Activity, adPlaceName: String) {
        if (!MobileAds.isInitialized) {
            notifyAdOpenAppFailedToShow(adPlaceName, true)
            return
        }
        if (!remoteConfigService.getRemoteConfigAdsActive()) {
            DataLogger.d("showAppOpenAdIfAvailable: AdActive = false")
            notifyAdOpenAppFailedToShow(adPlaceName, true)
            return
        }
        if (adManager.isNotAbleToVisibleAdsToUser(adPlaceName)) {
            notifyAdNotValidOrLoadFailed(adPlaceName)
            return
        }

        val adPlace = remoteConfigRepository.getAdPlaceBy(adPlaceName)
        val adHolder = getOrCreateAdHolderBy(adPlace)

        if (!isShowingAd && adHolder.isAdAvailable()) {
            if (!isTimeAvailableShowAds()) {
                notifyAdNotValidOrLoadFailed(adPlaceName)
                return
            }
            applicationScope.launch {
                val delayBeforeShowAppOpenAdTime = (adHolder.adPlace as AppOpenAdPlace).appOpenDelayBeforeShowMillis
                DataLogger.d( "AppOpenAd $adPlaceName delay before show $delayBeforeShowAppOpenAdTime")
                delay(delayBeforeShowAppOpenAdTime)
                DataLogger.d( "AppOpenAd $adPlaceName start show ")
                isShowingAd = true
                adHolder.appOpenAd?.adEventCallback = object : AppOpenAdEventCallback {
                    override fun onAdPaid(value: AdValue) {
                        super.onAdPaid(value)
//                        adManager.sendAdRevenueInfoToAdjust(value, adHolder.appOpenAd?.getResponseInfo()?.loadedAdSourceResponseInfo)
                    }

                    override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                        super.onAdFailedToShowFullScreenContent(fullScreenContentError)
                        DataLogger.d( "AppOpenAd $adPlaceName failed to show")
                        isShowingAd = false
                        var canRetryToShow = false
                        if (fullScreenContentError.code != FullScreenContentError.ErrorCode.APP_NOT_FOREGROUND) {
                            adHolder.reset()
                            fetchAd(activity, AdPlaceName.OPEN_APP)
                        } else {
                            canRetryToShow = true
                        }
                        applicationScope.launch {
//                            activity.removeDimForReopenApp()
                            notifyAdOpenAppFailedToShow(adPlaceName, canRetryToShow)
                        }
                    }

                    override fun onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent()
                        DataLogger.d( "AppOpenAd $adPlaceName showed")
                        isAppOpenAdDismissedDelivered = false
                        adPlaceNotDismissedDelivered = adPlaceName
                        applicationScope.launch {
                            notifyAdOpenAppShowing(adPlaceName)
                        }
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                    }

                    override fun onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent()
                        DataLogger.d( "AppOpenAd $adPlaceName dismissed")
                        PreventShowManyInterstitialAds.updateLastTimeShowedAppOpenAd()
                        applicationScope.launch {
                            isShowingAd = false
                            adHolder.reset()
                            notifyAdOpenAppDismissed(adPlaceName)
                            fetchAd(activity, AdPlaceName.OPEN_APP)
                        }
                    }
                }
                adHolder.appOpenAd?.setImmersiveMode(true)
                adHolder.appOpenAd?.show(activity)
                cancelCountDownTimer()
            }
        } else {
            if (context.isNetworkConnected()) {
                fetchAd(activity, adPlaceName)
            } else {
                notifyAdNotValidOrLoadFailed(adPlaceName)
            }
        }
    }

    private fun startCountDownTimerIfNeed() {
        val numMilliSecondsPerHour = 3600000L
        cancelCountDownTimer()
        /* countDownTimer = object : SonicCountDownTimer(numMilliSecondsPerHour * 4, 1000) {
             override fun onTimerTick(timeRemaining: Long) {}

             override fun onTimerFinish() {
                 adHolderMap.values.forEach { holder ->
                     holder.reset()
                 }
                 currentActivity?.let {
                     fetchAd(it, AdPlaceName.APP_REOPEN)
                 }
             }
         }
         countDownTimer?.startCountDownTimer()*/
    }

    fun isAppOpenAdLoadedAndAvailableToShow(placeName: String): Boolean {
        if (adManager.isNotAbleToVisibleAdsToUser(placeName)) {
            return false
        }
        val adPlace = adManager.getAdPlaceBy(placeName)
        return getOrCreateAdHolderBy(adPlace).isAdLoaded()
    }

    private fun cancelCountDownTimer() {
        /*if (countDownTimer != null) {
            countDownTimer?.pauseCountDownTimer()
            countDownTimer = null
        }*/
    }

    private fun isTimeAvailableShowAds(): Boolean {
        val numMilliSecondsPerHour =
            remoteConfigRepository.getConfigAdsAppOpen().timeInterval
        return getCurrentTimeInSecond() - PreventShowManyInterstitialAds.getLastTimeShowedAppOpenAd() >= numMilliSecondsPerHour
    }

    private fun getOrCreateAdHolderBy(adPlace: AdPlace): AppOpenAdHolder {
        var adHolder = adHolderMap[adPlace.placeName]
        if (adHolder == null || adHolder.adPlace.adType != adPlace.adType) {
            adHolder = AppOpenAdHolder(adPlace = adPlace)
            adHolderMap[adPlace.placeName] = adHolder
        }
        adHolder.adPlace = adPlace
        return adHolder
    }

    private fun notifyAdOpenAppLoaded(adPlaceName: String) {
        applicationScope.launch {
            _adOpenAppFlow.emit(AdOpenAdUiResource.AdLoaded(adPlaceName))
        }
    }

    private fun notifyAdOpenAppShowing(adPlaceName: String) {
        applicationScope.launch {
            _adOpenAppFlow.emit(AdOpenAdUiResource.AdShowing(adPlaceName))
        }
    }

    private fun notifyAdNotValidOrLoadFailed(adPlaceName: String) {
        applicationScope.launch {
            _adOpenAppFlow.emit(AdOpenAdUiResource.AdNotValidOrLoadFailed(adPlaceName))
        }
    }

    private fun notifyAdOpenAppDismissed(adPlaceName: String) {
        applicationScope.launch {
            _adOpenAppFlow.emit(AdOpenAdUiResource.AdDismissed(adPlaceName))
        }
    }

    private fun notifyAdOpenAppFailedToShow(adPlaceName: String, canRetryToShow: Boolean) {
        applicationScope.launch {
            _adOpenAppFlow.emit(AdOpenAdUiResource.AdFailedToShow(adPlaceName, canRetryToShow))
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        if (!isShowingAd) {
            currentActivity = activity
        }
    }

    override fun onActivityResumed(activity: Activity) {}

    override fun onActivityPaused(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {
        // Clear reference khi activity bị destroy để tránh leak — đặc biệt với
        // AdActivity của Google Mobile Ads SDK (start/destroy ngắn ngủi khi show
        // ad). Nếu không clear, currentActivity giữ AdActivity đã destroy.
        if (currentActivity === activity) {
            currentActivity = null
        }
    }

    /** Schedule 15s watchdog cho `isWaitLoadToShow` flow của AppOpenAd. Nếu
     *  load không complete trong 15s → notify failed + reset adHolder. */
    private fun scheduleWaitLoadTimeout(adPlaceName: String, adHolder: AppOpenAdHolder) {
        cancelWaitLoadTimeout(adPlaceName)
        waitLoadTimeoutJobs[adPlaceName] = applicationScope.launch {
            delay(WAIT_LOAD_TIMEOUT_MS.milliseconds)
            if (!adHolder.isWaitLoadToShow) return@launch
            notifyAdNotValidOrLoadFailed(adPlaceName)
            adHolder.reset()
        }
    }

    private fun cancelWaitLoadTimeout(adPlaceName: String) {
        waitLoadTimeoutJobs.remove(adPlaceName)?.cancel()
    }
}