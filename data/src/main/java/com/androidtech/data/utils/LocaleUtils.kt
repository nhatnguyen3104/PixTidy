package com.androidtech.data.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.os.LocaleListCompat.forLanguageTags
import com.androidtech.domain.model.settings.AppLanguage

/**
 * Sets the app's active locale using `AppCompatDelegate.setApplicationLocales()`. Since
 * androidx.appcompat 1.6.0 this single call already does exactly what the spec asks for:
 *  - On API 33+, it delegates to the framework's per-app `LocaleManager` (the real Android 13+
 *    Per-App Language Preferences API) — the OS itself persists the choice and no manual
 *    Context-wrapping is needed.
 *  - Below API 33, AppCompatDelegate persists the choice itself (via its own service-backed
 *    storage) and recreates activities that extend `AppCompatActivity` automatically, so strings
 *    re-render immediately without a manual `Activity.recreate()` call or a custom
 *    `attachBaseContext` Context wrapper.
 *
 * Safe to call from any thread as of appcompat 1.6.0. A plain top-level object (no DI) so it can
 * also be called from `Activity.attachBaseContext()` — which runs before Hilt field injection
 * completes on an `@AndroidEntryPoint` Activity — to apply the persisted locale as early as
 * possible on cold start, before the first Composition.
 */
object LocaleUtils {

    fun applyLocale(language: AppLanguage) {
        val localeList = language.languageTag
            ?.let { tag -> forLanguageTags(tag) }
            ?: LocaleListCompat.getEmptyLocaleList() // AppLanguage.SYSTEM: clear the override, follow OS/app default

        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /** Currently-applied locales, e.g. for showing "current: Tiếng Việt" before a settings Flow first emits. */
    fun currentAppLocales(): LocaleListCompat = AppCompatDelegate.getApplicationLocales()
}