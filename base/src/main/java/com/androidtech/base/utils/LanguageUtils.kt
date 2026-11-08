package com.androidtech.base.utils

import androidx.appcompat.app.AppCompatDelegate
import com.androidtech.base.model.LanguageModel
import java.util.Locale

fun getCurrentAppLanguageCode(): LanguageModel? {
    val appLocales = AppCompatDelegate.getApplicationLocales()

    return if (!appLocales.isEmpty) {
        LanguageModel.getSupportedLanguageByCode(appLocales.get(0)?.language ?: Locale.getDefault().language)
    } else {
        LanguageModel.getSupportedLanguageByCode(Locale.getDefault().language)
    }
}