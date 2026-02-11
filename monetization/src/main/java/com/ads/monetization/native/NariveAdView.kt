package com.ads.monetization.native

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

@Composable
fun NativeAdView(
    modifier: Modifier = Modifier,
    adUnitId: String,
    onAdLoaded: () -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {},
    customView: ((NativeAdView, NativeAd) -> Unit)? = null
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    val nativeAdLoader = remember { NativeAdLoader(adUnitId) }

    LaunchedEffect(Unit) {
        nativeAdLoader.loadAd(
            activity = context as Activity,
            onAdLoaded = { ad ->
                nativeAd = ad
                onAdLoaded()
            },
            onAdFailedToLoad = onAdFailedToLoad
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            nativeAdLoader.destroy()
        }
    }

    nativeAd?.let { mNativeAd ->
        AndroidView(
            modifier = modifier,
            factory = { cxt ->
                NativeAdView(cxt).apply {
                    if (customView != null) {
                        customView(this, mNativeAd)
                    } else {
                        setupDefaultNativeAdView(this, mNativeAd)
                    }
                }
            }
        )
    }
}

private fun setupDefaultNativeAdView(adView: NativeAdView, nativeAd: NativeAd) {
    adView.apply {
        headlineView = android.widget.TextView(context).apply {
            text = nativeAd.headline
            textSize = 16f
        }

        bodyView = android.widget.TextView(context).apply {
            text = nativeAd.body
            textSize = 14f
        }

        callToActionView = android.widget.Button(context).apply {
            text = nativeAd.callToAction
        }

        iconView = android.widget.ImageView(context).apply {
            nativeAd.icon?.let { setImageDrawable(it.drawable) }
        }

        setNativeAd(nativeAd)
    }
}