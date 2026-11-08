package com.androidtech.data.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.os.LocaleListCompat.forLanguageTags

object LocaleUtils {

    fun applyLocale(languageCode: String) {
        val localeList = if (languageCode.isEmpty() || languageCode.equals("system", ignoreCase = true)) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            forLanguageTags(languageCode)
        }

        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /** Currently-applied locales, e.g. for showing current app locale before a settings Flow first emits. */
    fun currentAppLocales(): LocaleListCompat = AppCompatDelegate.getApplicationLocales()
}
