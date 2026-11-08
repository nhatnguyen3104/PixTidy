package com.androidtech.data.model.configads

import android.os.CountDownTimer
import com.androidtech.data.utils.getCurrentTimeInSecond
import com.androidtech.domain.model.configad.ConfigAdsInterstitial

object PreventShowManyInterstitialAds {
    private var showInterAdLastTime = 0L

    private var showOpenAdLastTime = 0L

    private var timeInterval = 0L

    private var timeIntervalAfterShowOpenAd = 0L

    private var startTimeSession = 0L

    private var adShownInSessionCount = 0

    private var countDownTimer: CountDownTimer? = null

    fun initIntervalTimeShowInterstitialMillis(
        configAdsInterstitial: ConfigAdsInterstitial,
    ) {
        showInterAdLastTime = 0
        showOpenAdLastTime = 0
        timeInterval = configAdsInterstitial.timeInterval
        timeIntervalAfterShowOpenAd = configAdsInterstitial.timeIntervalAfterShowOpenAd
    }

    fun updateLastTimeShowedInterAd() {
        showInterAdLastTime = getCurrentTimeInSecond()
    }

    fun updateLastTimeShowedAppOpenAd() {
        showOpenAdLastTime = getCurrentTimeInSecond()
    }

    fun getLastTimeShowedInterAd() = showInterAdLastTime

    fun getLastTimeShowedAppOpenAd() = showOpenAdLastTime

    fun increaseNumberOfShowingInterAdInSession() {
        adShownInSessionCount++
    }

    fun startCountDownTimerIfNeed(timePerSession: Long) {
        if (startTimeSession != 0L) {
            return
        }
        startTimeSession = getCurrentTimeInSecond()
        if (countDownTimer != null) {
            countDownTimer?.cancel()
            countDownTimer = null
        }
        countDownTimer = object : CountDownTimer(timePerSession * 1000L, 1000) {
            override fun onTick(secondUntilFinished: Long) {}

            override fun onFinish() {
                resetInterAdSession()
            }
        }
        countDownTimer?.start()
    }

    fun isNotValidIntervalTimeShowAds(): Boolean {
        val timeInterActive = getCurrentTimeInSecond() - showInterAdLastTime
        val timeOpenAdActive = getCurrentTimeInSecond() - showOpenAdLastTime
        return if (showOpenAdLastTime > showInterAdLastTime) {
            timeOpenAdActive < timeIntervalAfterShowOpenAd
        } else {
            timeInterActive < timeInterval
        }
    }

    fun isNotValidSessionTimeShowAds(interstitialAdConfig: ConfigAdsInterstitial): Boolean {
        return adShownInSessionCount >= interstitialAdConfig.maxShowPerSession
    }

    private fun resetInterAdSession() {
        startTimeSession = 0L
        adShownInSessionCount = 0
    }
}