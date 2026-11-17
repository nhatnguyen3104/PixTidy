package com.androidtech.generalapps.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.androidtech.base.logger.Logger
import com.androidtech.data.logger.DataLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApp: Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Logger.init()
        DataLogger.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}