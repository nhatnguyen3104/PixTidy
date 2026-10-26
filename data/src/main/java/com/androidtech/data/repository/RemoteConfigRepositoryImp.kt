package com.androidtech.data.repository

import android.content.Context
import android.widget.Toast
import com.androidtech.data.R
import com.androidtech.data.logger.DataLogger
import com.androidtech.data.model.remoteconfig.ConfigAdsAppOpen
import com.androidtech.data.model.remoteconfig.ConfigAdsBanner
import com.androidtech.data.model.remoteconfig.ConfigAdsInterstitial
import com.androidtech.data.model.remoteconfig.ConfigAdsModel
import com.androidtech.data.model.remoteconfig.ConfigAdsNative
import com.androidtech.data.model.remoteconfig.ConfigAdsRewarded
import com.androidtech.data.remoteconfig.RemoteConfigService
import com.androidtech.data.utils.Constants
import com.androidtech.data.utils.isDebug
import com.androidtech.domain.model.configad.ConfigFormatAdId
import com.androidtech.domain.repository.RemoteConfigRepository
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

class RemoteConfigRepositoryImp @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigService: RemoteConfigService
): RemoteConfigRepository {

    private val remoteConfigScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isFetchComplete = false
    private val computeMutex = Mutex()
    private val remoteConfig by lazy {
        val settings = remoteConfigSettings {
            fetchTimeoutInSeconds = 30
            minimumFetchIntervalInSeconds = 0
        }
        Firebase.remoteConfig.apply {
            setConfigSettingsAsync(settings)
            setDefaultsAsync(R.xml.remote_configs_default)
        }
    }


    private var configFormatAdIdCache: ConfigFormatAdId? = null
    private var configAdsActiveCache: Boolean? = null
    private var configAdsBannerCache: ConfigAdsBanner? = null
    private var configAdsNativeCache: ConfigAdsNative? = null
    private var configAdsInterstitialCache: ConfigAdsInterstitial? = null
    private var configAdsRewardedCache: ConfigAdsRewarded? = null
    private var configAdsAppOpenCache: ConfigAdsAppOpen? = null

    override fun fetchAndActive() {
        remoteConfigScope.launch {
            isFetchComplete = false
            DataLogger.d("Fetch remoteConfig loading")
            remoteConfig.fetchAndActivate()
                .addOnSuccessListener { isSuccess ->
                    if (context.isDebug()) {
                        Toast.makeText(
                            context,
                            "Fetch Remote config is Successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    fetchRemoteConfigData()
                }
                .addOnFailureListener { exception ->

                }
            delay(Constants.FETCH_REMOTE_CONFIG_TIMEOUT)
            if (!isFetchComplete) {
                fetchRemoteConfigData()
            }
        }
    }

    override fun getConfigAdsActive(): Flow<Boolean> = flow {
        emit(configAdsActiveCache ?: true)
    }

    override fun getConfigAd(): Flow<ConfigFormatAdId?> = flow {
        emit(configFormatAdIdCache)
    }

    private fun fetchRemoteConfigData() {
        remoteConfigScope.launch {
            computeMutex.lock()
            try {
                val configFormatAdIdDeferred = async { getConfigFormatAdIdDefaultRaw() }
                val configAdsBannerDeferred = async { getConfigAdsBannerRaw() }
                val configAdsNativeDeferred = async { getConfigAdsNativeRaw() }
                val configAdsInterstitialDeferred = async { getConfigAdsInterstitial() }
                val configAdsRewardedDeferred = async { getConfigAdsRewardedRaw() }
                val configAdsAppOpenDeferred = async { getConfigAdsAppOpen() }

                configFormatAdIdCache = configFormatAdIdDeferred.await()
                configAdsActiveCache = getConfigAdsActiveRaw()

                configAdsBannerCache = configAdsBannerDeferred.await()
                configAdsNativeCache = configAdsNativeDeferred.await()
                configAdsInterstitialCache = configAdsInterstitialDeferred.await()
                configAdsRewardedCache = configAdsRewardedDeferred.await()
                configAdsAppOpenCache = configAdsAppOpenDeferred.await()

                isFetchComplete = true
                DataLogger.d("fetchRemoteConfigData successfully: ${true}")
            } catch (e: Exception) {
                isFetchComplete = false
                DataLogger.e("fetchRemoteConfigData error: ${e.message}")
            } finally {
                computeMutex.unlock()
            }
        }
    }

    private fun getConfigFormatAdIdDefaultRaw(): ConfigFormatAdId {
        val model = remoteConfigService.getFormatAdIdModel()
        return model?.transform() ?: ConfigFormatAdId()
    }

    private fun getConfigAdsBannerRaw(): ConfigAdsBanner {
        val model = remoteConfigService.getConfigBannerAdsModel()
        return model?.toAdsBanner() ?: ConfigAdsModel.toDefaultAdsBanner()
    }

    private fun getConfigAdsNativeRaw(): ConfigAdsNative {
        val model = remoteConfigService.getConfigNativeAdsModel()
        return model?.toAdsNative() ?: ConfigAdsModel.toDefaultAdsNative()
    }

    private fun getConfigAdsInterstitial(): ConfigAdsInterstitial {
        val model = remoteConfigService.getConfigInterstitialAdsModel()
        return model?.toAdsInterstitial() ?: ConfigAdsModel.toDefaultAdsInterstitial()
    }

    private fun getConfigAdsRewardedRaw(): ConfigAdsRewarded {
        val model = remoteConfigService.getConfigRewardedAdsModel()
        return model?.toAdsRewarded() ?: ConfigAdsModel.toDefaultAdsRewarded()
    }

    private fun getConfigAdsAppOpen(): ConfigAdsAppOpen {
        val model = remoteConfigService.getConfigAppOpenAdModel()
        return model?.toAdsAppOpen() ?: ConfigAdsModel.toDefaultAdsAppOpen()
    }

    private fun getConfigAdsActiveRaw(): Boolean {
        return remoteConfigService.getRemoteConfigAdsActive() ?: true
    }
}