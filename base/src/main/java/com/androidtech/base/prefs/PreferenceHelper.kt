package com.androidtech.base.prefs

import android.content.Context
import com.androidtech.data.local.prefs.Preference
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PreferenceHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) : Preference(context) {
    var isFirstOpen by booleanRef("is_first_open", true)
    var languageCode by stringRef("language_code", "")
    var timeOfFirstAdClicked by longRef("time_off_first_ad_clicked", 0L)
}