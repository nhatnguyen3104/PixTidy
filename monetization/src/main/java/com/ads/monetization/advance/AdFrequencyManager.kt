package com.ads.monetization.advance

import android.content.Context
import com.ads.monetization.config.AdType

class AdFrequencyManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("ad_frequency", Context.MODE_PRIVATE)

    companion object {
        private const val INTERSTITIAL_INTERVAL_MS = 5 * 60 * 1000L // 5 phút
        private const val MAX_INTERSTITIAL_PER_SESSION = 3
        private const val KEY_LAST_SHOWN = "last_shown_"
        private const val KEY_SHOWN_COUNT = "shown_count_"
    }

    fun canShowAd(adType: AdType): Boolean {
        val now = System.currentTimeMillis()
        val lastShown = prefs.getLong(KEY_LAST_SHOWN + adType.name, 0)
        val shownCount = prefs.getInt(KEY_SHOWN_COUNT + adType.name, 0)

        return when (adType) {
            AdType.INTERSTITIAL -> {
                val timePassed = now - lastShown >= INTERSTITIAL_INTERVAL_MS
                val underLimit = shownCount < MAX_INTERSTITIAL_PER_SESSION
                timePassed && underLimit
            }
            AdType.REWARDED -> true // Luôn cho phép rewarded ad
            AdType.APP_OPEN -> {
                // Không show quá thường xuyên
                now - lastShown >= 4 * 60 * 60 * 1000L // 4 giờ
            }
            else -> true
        }
    }

    fun recordAdShown(adType: AdType) {
        val now = System.currentTimeMillis()
        val currentCount = prefs.getInt(KEY_SHOWN_COUNT + adType.name, 0)

        prefs.edit()
            .putLong(KEY_LAST_SHOWN + adType.name, now)
            .putInt(KEY_SHOWN_COUNT + adType.name, currentCount + 1)
            .apply()
    }

    fun resetSession() {
        prefs.edit()
            .clear()
            .apply()
    }
}