package com.androidtech.generalapps.app

import android.app.Application
import com.androidtech.base.logger.Logger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Logger.init()
    }
}