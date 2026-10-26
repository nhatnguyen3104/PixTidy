package com.androidtech.data.utils

import android.content.Context
import android.content.pm.ApplicationInfo

fun Context.isDebug() = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0