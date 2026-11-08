package com.androidtech.data.repository

import android.content.Context
import android.widget.Toast
import com.androidtech.data.R
import com.androidtech.data.logger.DataLogger
import com.androidtech.data.model.adplace.mapperModel
import com.androidtech.data.model.formatadid.mapperData
import com.androidtech.data.model.mapper.mapperAppOpenAd
import com.androidtech.data.model.mapper.mapperBannerAd
import com.androidtech.data.model.mapper.mapperInterstitialAd
import com.androidtech.data.model.mapper.mapperModel
import com.androidtech.data.model.mapper.mapperNativeAd
import com.androidtech.data.model.mapper.mapperRewardedAd
import com.androidtech.data.model.mapper.mapperRewardedInterstitialAd
import com.androidtech.data.remoteconfig.CountryTier
import com.androidtech.data.remoteconfig.RemoteConfigService
import com.androidtech.data.utils.Constants
import com.androidtech.data.utils.getCountryCode
import com.androidtech.data.utils.isDebug
import com.androidtech.domain.model.configad.AdPlace
import com.androidtech.domain.model.configad.ConfigAdsAppOpen
import com.androidtech.domain.model.configad.ConfigAdsNative
import com.androidtech.domain.model.configad.ConfigAdsBanner
import com.androidtech.domain.model.configad.ConfigAdsInterstitial
import com.androidtech.domain.model.configad.ConfigAdsRewarded
import com.androidtech.domain.model.configad.ConfigAdsRewardedInterstitial
import com.androidtech.domain.model.configad.ConfigFormatAdId
import com.androidtech.domain.model.configad.ConfigPreventAdClick
import com.androidtech.domain.model.configad.NoneAdPlace
import com.androidtech.domain.repository.RemoteConfigRepository
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.collections.map
import kotlin.concurrent.withLock
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

class RemoteConfigRepositoryImp @Inject constructor(
    @param:ApplicationContext private val context: Context,
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
    private var configCountriesDisableAdsCache = listOf<String>()
    private var configPreventAdClickCache: ConfigPreventAdClick? = null

    private val countriesTier1 by lazy {
        listOf(
            "US", "CA", "GB", "AU", "NZ", "IE",
            "DE", "FR", "AT", "CH", "BE", "NL", "LU",
            "SE", "NO", "FI", "DK",
            "IT", "ES",
            "JP", "KR", "SG"
        )
    }
    private val countriesTier2 by lazy {
        listOf(
            "PT", "PL", "CZ", "SK", "HU", "GR", "SI",
            "IL", "AE", "SA", "KW", "QA",
            "HK", "TW",
            "BR", "MX", "AR", "CL",
            "TR", "RU"
        )
    }

    private var configFormatAdIdCache: ConfigFormatAdId? = null
    private var configAdsActiveCache: Boolean? = null
    private var configAdsBannerCache: ConfigAdsBanner? = null
    private var configAdsNativeCache: ConfigAdsNative? = null
    private var configAdsInterstitialCache: ConfigAdsInterstitial? = null
    private var configAdsRewardedCache: ConfigAdsRewarded? = null
    private var configAdsRewardedInterstitialCache: ConfigAdsRewardedInterstitial? = null
    private var configAdsAppOpenCache: ConfigAdsAppOpen? = null

    private var adPlacesAppOpenCache: List<AdPlace>? = null
    private var adPlacesBannerCache: List<AdPlace>? = null
    private var adPlacesNativeCache: List<AdPlace>? = null
    private var adPlacesFullscreenCache: List<AdPlace>? = null

    override suspend fun fetchAndActive() {
        if (isFetchComplete) return
        computeMutex.withLock {
            if (isFetchComplete) return@withLock
            DataLogger.d("Fetch remoteConfig loading")
            val isSuccess = withTimeoutOrNull(Constants.FETCH_REMOTE_CONFIG_TIMEOUT.milliseconds) {
                try {
                    suspendCancellableCoroutine { continuation ->
                        remoteConfig.fetchAndActivate()
                            .addOnCompleteListener { task ->
                                if (continuation.isActive) {
                                    continuation.resume(task.isSuccessful && task.result == true)
                                }
                            }
                    }
                } catch (e: Exception) {
                    DataLogger.e("fetchAndActive exception: ${e.message ?: e.toString()}")
                    false
                }
            } ?: false

            if (context.isDebug() && isSuccess) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Fetch Remote config is Successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            fetchRemoteConfigData()
        }
    }

    private suspend fun fetchRemoteConfigData() = coroutineScope {
        try {
            val configCountryTierDeferred = async { getConfigCountryTierRaw() }
            val countryTier = configCountryTierDeferred.await()
            val configCountriesDisableAdsDeferred = async { getConfigCountriesDisableAdsRaw() }
            val configPreventAdClickDeferred = async { getConfigPreventAdClickRaw() }

            val configFormatAdIdDeferred = async { getConfigFormatAdIdDefaultRaw(countryTier) }
            val configAdsBannerDeferred = async { getConfigAdsBannerRaw(countryTier) }
            val configAdsNativeDeferred = async { getConfigAdsNativeRaw(countryTier) }
            val configAdsInterstitialDeferred = async { getConfigAdsInterstitialRaw(countryTier) }
            val configAdsRewardedDeferred = async { getConfigAdsRewardedRaw(countryTier) }
            val configAdsRewardedInterstitialDeferred = async { getConfigAdsRewardedInterstitialRaw(countryTier) }
            val configAdsAppOpenDeferred = async { getConfigAdsAppOpenRaw(countryTier) }

            val configFormatAdId = configFormatAdIdDeferred.await()
            configFormatAdIdCache = configFormatAdId

            val adPlacesAppOpenDeferred = async { getAdPlacesAppOpenRaw(countryTier, configFormatAdId) }
            val adPlacesBannerDeferred = async { getAdPlacesBannerRaw(countryTier, configFormatAdId) }
            val adPlacesNativeDeferred = async { getAdPlacesNativeRaw(countryTier, configFormatAdId) }
            val adPlacesFullscreenDeferred = async { getAdPlacesFullScreenRaw(countryTier, configFormatAdId) }

            configAdsActiveCache = getConfigAdsActiveRaw()

            configAdsBannerCache = configAdsBannerDeferred.await()
            configAdsNativeCache = configAdsNativeDeferred.await()
            configAdsInterstitialCache = configAdsInterstitialDeferred.await()
            configAdsRewardedCache = configAdsRewardedDeferred.await()
            configAdsAppOpenCache = configAdsAppOpenDeferred.await()
            configAdsRewardedInterstitialCache = configAdsRewardedInterstitialDeferred.await()

            adPlacesAppOpenCache = adPlacesAppOpenDeferred.await()
            adPlacesBannerCache = adPlacesBannerDeferred.await()
            adPlacesNativeCache = adPlacesNativeDeferred.await()
            adPlacesFullscreenCache = adPlacesFullscreenDeferred.await()

            configCountriesDisableAdsCache = configCountriesDisableAdsDeferred.await()
            configPreventAdClickCache = configPreventAdClickDeferred.await()

            isFetchComplete = true
            DataLogger.d("fetchRemoteConfigData successfully: ${true}")
        } catch (e: Exception) {
            isFetchComplete = false
            DataLogger.e("fetchRemoteConfigData error: ${e.message ?: e.toString()}")
        }
    }

    override fun getConfigAdsActive(): Flow<Boolean> = flow {
        emit(configAdsActiveCache ?: true)
    }

    override fun getConfigAd(): Flow<ConfigFormatAdId?> = flow {
        emit(configFormatAdIdCache)
    }

    override fun getAdPlaceBy(adPlaceName: String): AdPlace {
        return getAdPlaces().find { it.placeName == adPlaceName } ?: NoneAdPlace(placeName = adPlaceName)
    }

    override fun getAdPlaces(): List<AdPlace> {
        val countryTier = getConfigCountryTierRaw()
        val formatId = configFormatAdIdCache ?: getConfigFormatAdIdDefaultRaw(countryTier)

        val appOpen = adPlacesAppOpenCache ?: getAdPlacesAppOpenRaw(countryTier, formatId)
        val banner = adPlacesBannerCache ?: getAdPlacesBannerRaw(countryTier, formatId)
        val native = adPlacesNativeCache ?: getAdPlacesNativeRaw(countryTier, formatId)
        val fullscreen = adPlacesFullscreenCache ?: getAdPlacesFullScreenRaw(countryTier, formatId)

        return buildList {
            addAll(appOpen)
            addAll(banner)
            addAll(native)
            addAll(fullscreen)
        }
    }

    override fun getConfigCountriesDisableAds(): List<String> {
        return remoteConfigService.getConfigCountriesDisableAds() ?: emptyList()
    }

    override fun getConfigPreventAdClick(): ConfigPreventAdClick {
        return configPreventAdClickCache ?: getConfigPreventAdClickRaw()
    }

    override fun getConfigAdsBanner(): ConfigAdsBanner {
        return configAdsBannerCache ?: getConfigAdsBannerRaw(getConfigCountryTierRaw())
    }

    override fun getConfigAdsNative(): ConfigAdsNative {
        return configAdsNativeCache ?: getConfigAdsNativeRaw(getConfigCountryTierRaw())
    }

    override fun getConfigAdsAppOpen(): ConfigAdsAppOpen {
        return configAdsAppOpenCache ?: getConfigAdsAppOpenRaw(getConfigCountryTierRaw())
    }

    override fun getConfigAdsInterstitial(): ConfigAdsInterstitial {
        return configAdsInterstitialCache ?: getConfigAdsInterstitialRaw(getConfigCountryTierRaw())
    }

    override fun getConfigAdsRewardedInterstitial(): ConfigAdsRewardedInterstitial {
        return configAdsRewardedInterstitialCache ?: getConfigAdsRewardedInterstitialRaw(getConfigCountryTierRaw())
    }

    override fun getConfigAdsRewarded(): ConfigAdsRewarded {
        return configAdsRewardedCache ?: getConfigAdsRewardedRaw(getConfigCountryTierRaw())
    }

    private fun getConfigCountryTierRaw(): CountryTier {
        val country = context.applicationContext.getCountryCode().uppercase()
        if (country.isBlank()) {
            return CountryTier.Default
        }
        if (countriesTier1.contains(country)) {
            return CountryTier.Tier1
        }
        if (countriesTier2.contains(country)) {
            return CountryTier.Tier2
        }
        return CountryTier.Default
    }

    private fun getConfigFormatAdIdDefaultRaw(countryTier: CountryTier): ConfigFormatAdId {
        val model = remoteConfigService.getFormatAdIdModel()
        return model?.mapperData(context, countryTier) ?: ConfigFormatAdId()
    }

    private fun getConfigAdsBannerRaw(countryTier: CountryTier): ConfigAdsBanner {
        val model = remoteConfigService.getConfigBannerAdsModel()
        return model?.mapperBannerAd(context, countryTier) ?: ConfigAdsBanner.default
    }

    private fun getConfigAdsNativeRaw(countryTier: CountryTier): ConfigAdsNative {
        val model = remoteConfigService.getConfigNativeAdsModel()
        return model?.mapperNativeAd(context, countryTier) ?: ConfigAdsNative.default
    }

    private fun getConfigAdsInterstitialRaw(countryTier: CountryTier): ConfigAdsInterstitial {
        val model = remoteConfigService.getConfigInterstitialAdsModel()
        return model?.mapperInterstitialAd(context, countryTier) ?: ConfigAdsInterstitial.default
    }

    private fun getConfigAdsRewardedRaw(countryTier: CountryTier): ConfigAdsRewarded {
        val model = remoteConfigService.getConfigRewardedAdsModel()
        return model?.mapperRewardedAd(context, countryTier) ?: ConfigAdsRewarded.default
    }

    private fun getConfigAdsRewardedInterstitialRaw(countryTier: CountryTier): ConfigAdsRewardedInterstitial {
        val model = remoteConfigService.getConfigRewardedInterstitialAdsModel()
        return model?.mapperRewardedInterstitialAd(context, countryTier) ?: ConfigAdsRewardedInterstitial.default
    }

    private fun getConfigAdsAppOpenRaw(countryTier: CountryTier): ConfigAdsAppOpen {
        val model = remoteConfigService.getConfigAppOpenAdModel()
        return model?.mapperAppOpenAd(context, countryTier) ?: ConfigAdsAppOpen.default
    }

    private fun getConfigAdsActiveRaw(): Boolean {
        return remoteConfigService.getRemoteConfigAdsActive() ?: false
    }

    private fun getAdPlacesAppOpenRaw(
        countryTier: CountryTier,
        configFormatAdId: ConfigFormatAdId
    ): List<AdPlace> {
        return remoteConfigService.getAdPlacesAppOpenModel().map { adPlaceModel ->
            adPlaceModel.mapperModel(context, countryTier, configFormatAdId)
        }
    }

    private fun getAdPlacesNativeRaw(
        countryTier: CountryTier,
        configFormatAdId: ConfigFormatAdId
    ): List<AdPlace> {
        return remoteConfigService.getAdPlacesNativeModel().map { adPlaceModel ->
            adPlaceModel.mapperModel(context, countryTier, configFormatAdId)
        }
    }

    private fun getAdPlacesFullScreenRaw(
        countryTier: CountryTier,
        configFormatAdId: ConfigFormatAdId
    ): List<AdPlace> {
        return remoteConfigService.getAdPlacesFullScreenModel().map { adPlaceModel ->
            adPlaceModel.mapperModel(context, countryTier, configFormatAdId)
        }
    }

    private fun getAdPlacesBannerRaw(
        countryTier: CountryTier,
        configFormatAdId: ConfigFormatAdId
    ): List<AdPlace> {
        return remoteConfigService.getAdPlacesBannerModel().map { adPlaceModel ->
            adPlaceModel.mapperModel(context, countryTier, configFormatAdId)
        }
    }

    private fun getConfigCountriesDisableAdsRaw(): List<String> {
        return remoteConfigService.getConfigCountriesDisableAds() ?: emptyList()
    }

    private fun getConfigPreventAdClickRaw(): ConfigPreventAdClick {
        val model = remoteConfigService.getConfigPreventAdClick()
        return model?.mapperModel() ?: ConfigPreventAdClick.default
    }
}