package com.ads.monetization.native

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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
import com.androidtech.base.logger.Logger
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import androidx.core.graphics.toColorInt

@Composable
fun NativeAdView(
    modifier: Modifier = Modifier,
    adUnitId: String,
    onAdLoaded: () -> Unit = {},
    onAdFailedToLoad: (LoadAdError) -> Unit = {},
    customView: ((NativeAdView, NativeAd) -> Unit)? = null,
    onAdClicked: () -> Unit = {}
) {
    val context = LocalContext.current
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }
    val nativeAdLoader = remember { NativeAdLoader(adUnitId) }

    LaunchedEffect(Unit) {
        nativeAdLoader.loadAd(
            context = context,
            onAdLoaded = { ad ->
                Logger.d("NativeAdView onAdLoaded")
                nativeAd = ad
                onAdLoaded()
            },
            onAdFailedToLoad = {
                Logger.d("NativeAdView LoadError: ${it.message}")
                onAdFailedToLoad(it)
            }
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
                        setupDefaultNativeAdView(this, mNativeAd, onAdClicked = onAdClicked)
                    }
                }
            }
        )
    }
}

private fun setupDefaultNativeAdView(
    adView: NativeAdView,
    nativeAd: NativeAd,
    onAdClicked: () -> Unit
) {
    val context = adView.context

    // Root container khớp với NativeAdView
    val rootLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setPadding(16, 16, 16, 16)
        setBackgroundColor("#1C1C1E".toColorInt())
    }

    val headerRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    }

    val iconView = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(80, 80).also { it.marginEnd = 12 }
        scaleType = ImageView.ScaleType.CENTER_CROP
        nativeAd.icon?.drawable?.let { setImageDrawable(it) }
    }
    adView.iconView = iconView

    val titleColumn = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f)
    }

    val headlineRow = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        gravity = Gravity.CENTER_VERTICAL
    }

    val adBadge = TextView(context).apply {
        text = "Ad"
        setTextColor(Color.BLACK)
        setBackgroundColor(Color.YELLOW)
        textSize = 10f
        setTypeface(null, android.graphics.Typeface.BOLD)
        setPadding(8, 2, 8, 2)
        layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).also {
            it.marginEnd = 8
        }
    }

    val headlineView = TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        text = nativeAd.headline
        textSize = 15f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setTextColor(Color.WHITE)
        maxLines = 2
    }
    adView.headlineView = headlineView
    headlineRow.addView(adBadge)
    headlineRow.addView(headlineView)

    val advertiserView = TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        text = nativeAd.advertiser ?: ""
        textSize = 12f
        setTextColor("#AAAAAA".toColorInt())
        visibility = if (nativeAd.advertiser != null) android.view.View.VISIBLE
        else android.view.View.GONE
    }
    adView.advertiserView = advertiserView

    titleColumn.addView(headlineRow)
    titleColumn.addView(advertiserView)
    headerRow.addView(iconView)
    headerRow.addView(titleColumn)
    rootLayout.addView(headerRow)

    // ── Row 2: Body text ─────────────────────────────────────────────────
    val bodyView = TextView(context).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
            it.topMargin = 8
        }
        text = nativeAd.body
        textSize = 13f
        setTextColor("#CCCCCC".toColorInt())
        maxLines = 3
    }
    adView.bodyView = bodyView
    rootLayout.addView(bodyView)

    // ── Row 3: MediaView ─────────────────────────────────────────────────
    val mediaView = MediaView(context).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f).also {
            it.topMargin = 10
        }.let {
            LinearLayout.LayoutParams(MATCH_PARENT, 400).also { lp -> lp.topMargin = 10 }
        }
        mediaContent = nativeAd.mediaContent!!
    }
    adView.mediaView = mediaView
    rootLayout.addView(mediaView)

    // ── Row 4: CTA Button ─────────────────────────────────────────────────
    val ctaButton = Button(context).apply {
        layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).also {
            it.topMargin = 10
        }
        text = nativeAd.callToAction
        textSize = 14f
        setTextColor(Color.WHITE)
        setBackgroundColor("#FF6B35".toColorInt())
        setOnClickListener {
            onAdClicked()
        }
    }
    adView.callToActionView = ctaButton
    rootLayout.addView(ctaButton)

    adView.setOnClickListener {
        onAdClicked()
    }

    adView.addView(rootLayout)
    adView.setNativeAd(nativeAd)
}