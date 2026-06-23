package com.ads.monetization.banner

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.androidtech.base.logger.Logger
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String,
    adSize: AdSize = AdSize.BANNER,
    onAdLoaded: () -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {}
) {
    val context = LocalContext.current
    val bannerAdLoader = remember { BannerAdLoader(adUnitId) }

    AndroidView(
        modifier = modifier,
        factory = { cxt ->
            bannerAdLoader.createAdView(
                activity = context as Activity,
                adSize = adSize,
                onAdLoaded = {
                    Logger.d("BannerAdView loaded")
                    onAdLoaded()
                },
                onAdFailedToLoad = {
                    Logger.e("BannerAdView failed to load: ${it.message}")
                    onAdFailedToLoad(it)
                }
            )
        },
        update = { adView ->
            if (adView.adUnitId != adUnitId) {
                adView.adUnitId = adUnitId
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            bannerAdLoader.destroy()
        }
    }
}