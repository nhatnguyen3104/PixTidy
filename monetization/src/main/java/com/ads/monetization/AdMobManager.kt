package com.ads.monetization

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ads.monetization.config.AdType
import com.ads.monetization.config.ConsentFromUiResource
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class AdMobManager @Inject constructor(
    private val context: Context
): AdManager {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var _requestConsentFlow = MutableSharedFlow<ConsentFromUiResource>()
    override val requestConsentFlow = _requestConsentFlow.asSharedFlow()

    override fun initializeMobileAdsSdk() {
        applicationScope.launch {
            try {
                MobileAds.initialize(context) { initializationStatus ->
                    val statusMap = initializationStatus.adapterStatusMap
                    statusMap.keys.forEach { adapterClass ->
                        val status = statusMap[adapterClass]
                        Log.d("MobileAds", "initializeMobileAdsSdk status: $status")
                    }
                }
                MobileAds.setAppVolume(0.5f)
                _requestConsentFlow.emit(ConsentFromUiResource.Complete)
            } catch (exception: Exception) {
                _requestConsentFlow.emit(ConsentFromUiResource.Complete)
            }
        }
    }

    override fun preloadFullScreenAd(
        activity: Activity,
        placeName: String,
        isNeedNotifyEvent: Boolean
    ) {

    }

    override fun preloadAds(activity: Activity) {
        TODO("Not yet implemented")
    }

    override fun preloadAd(
        activity: Activity,
        adType: AdType
    ) {
        TODO("Not yet implemented")
    }

    override fun showAdIfAvailable(
        activity: Activity,
        adType: AdType,
        onAdShown: () -> Unit,
        onAdFailed: (String) -> Unit,
        onAdClosed: () -> Unit
    ) {
        TODO("Not yet implemented")
    }

    override fun isAdLoaded(adType: AdType): Boolean {
        TODO("Not yet implemented")
    }

    override fun clearAd(adType: AdType) {
        TODO("Not yet implemented")
    }

    override fun clearAllAds() {
        TODO("Not yet implemented")
    }
}