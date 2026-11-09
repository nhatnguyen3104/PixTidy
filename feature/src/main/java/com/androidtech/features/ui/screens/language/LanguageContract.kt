package com.androidtech.features.ui.screens.language

import android.app.Activity
import com.ads.monetization.adholder.NativeAdHolder
import com.androidtech.base.model.LanguageModel
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIIntent
import com.androidtech.base.viewmodel.UIState
import com.androidtech.features.ui.screens.splash.SplashEvent

sealed interface LanguageEvent: UIEvent {
    data class ClickLanguageItem(val languageModel: LanguageModel) : LanguageEvent
    data object ClickToSaveLanguage : LanguageEvent
    data class PreloadAds(val activity: Activity): LanguageEvent
}


data class LanguageState(
    val allLanguages: List<LanguageModel> = LanguageModel.allLanguages,
    val saveLanguageModel: LanguageModel = LanguageModel.ENGLISH,
    val isShowLoading: Boolean = false,
    val nativeHolder: NativeAdHolder? = null,
    val adLoadTrigger: Int = 0
): UIState

sealed interface LanguageEffect: UIEffect {
    data object NavigateToHome: LanguageEffect
    data object NavigateToOnboarding: LanguageEffect
    data class ApplyLanguage(val languageCode: String): LanguageEffect
}