package com.androidtech.data.logger

import android.content.Context
import com.androidtech.data.utils.isDebug
import timber.log.Timber

object DataLogger {
    private const val TAG = "ModernApp"

    fun d(s: String?, vararg objects: Any?) {
        Timber.tag(TAG).d(s, *objects)
    }

    fun d(throwable: Throwable, s: String?, vararg objects: Any?) {
        Timber.tag(TAG).d(throwable, s, objects)
    }

    fun e(s: String?, vararg objects: Any?) {
        Timber.tag(TAG).e(s, objects)
    }

    fun e(throwable: Throwable, s: String?, vararg objects: Any?) {
        Timber.tag(TAG).e(throwable, s, objects)
    }

    fun init(context: Context) {
        if (context.isDebug()) { Timber.plant(Timber.DebugTree()) }
    }
}