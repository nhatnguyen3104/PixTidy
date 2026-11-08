package com.androidtech.features.ui.screens.language

import com.androidtech.base.model.LanguageModel
import com.androidtech.base.viewmodel.UIEffect
import com.androidtech.base.viewmodel.UIEvent
import com.androidtech.base.viewmodel.UIIntent
import com.androidtech.base.viewmodel.UIState

sealed interface LanguageEvent: UIEvent {
    data class ClickLanguageItem(val languageModel: LanguageModel) : LanguageEvent
    data object ClickToSaveLanguage : LanguageEvent
}


data class LanguageState(
    val allLanguages: List<LanguageModel> = LanguageModel.allLanguages,
    val saveLanguageModel: LanguageModel = LanguageModel.ENGLISH,
    val isShowLoading: Boolean = false
): UIState

sealed interface LanguageEffect: UIEffect {
    data object NavigateToHome: LanguageEffect
    data object NavigateToOnboarding: LanguageEffect
    data class ApplyLanguage(val languageCode: String): LanguageEffect
}