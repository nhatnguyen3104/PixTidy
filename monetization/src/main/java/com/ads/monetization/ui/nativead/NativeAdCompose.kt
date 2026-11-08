package com.ads.monetization.ui.nativead

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.androidtech.base.theme.AdBadgesBackgroundColor
import com.androidtech.base.theme.Body14Regular
import com.androidtech.base.theme.Body14SemiBold
import com.androidtech.base.theme.Body16Regular
import com.androidtech.base.theme.Body16SemiBold
import com.androidtech.base.theme.NativeAdBodyColor
import com.androidtech.base.theme.NativeAdTitleColor
import com.androidtech.base.theme.PrimaryBlue
import com.androidtech.base.theme.SurfaceWhite
import com.ads.monetization.adholder.NativeAdHolder
import com.google.android.libraries.ads.mobile.sdk.common.AdChoicesView
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView

val LocalNativeAd = staticCompositionLocalOf<NativeAd?> { null }
val LocalNativeAdView = staticCompositionLocalOf<NativeAdView?> { null }
val LocalMediaViewRegister = staticCompositionLocalOf<(MediaView?) -> Unit> { {} }

@Stable
class NativeAdAssetRegistry {
    private val attached = mutableSetOf<View>()

    var epoch by mutableIntStateOf(0)
        private set

    fun attach(view: View) {
        if (attached.add(view)) epoch++
    }
}

val LocalNativeAdAssetRegistry = staticCompositionLocalOf { NativeAdAssetRegistry() }

val SMALL_NATIVE_AD_HEIGHT = 88.dp
val MEDIUM_NATIVE_AD_HEIGHT = 260.dp

@Composable
fun SmallNativeAdView(adHolder: NativeAdHolder?, modifier: Modifier = Modifier) {
    val ad = adHolder?.nativeAd ?: return
    SmallNativeAdView(ad = ad, modifier = modifier)
}

@Composable
fun MediumNativeAdView(adHolder: NativeAdHolder?, modifier: Modifier = Modifier) {
    val ad = adHolder?.nativeAd ?: return
    MediumNativeAdView(ad = ad, modifier = modifier)
}

@Composable
fun FullNativeAdView(adHolder: NativeAdHolder?, modifier: Modifier = Modifier) {
    val ad = adHolder?.nativeAd ?: return
    FullNativeAdView(ad = ad, modifier = modifier)
}

@Composable
private fun rememberIconBitmap(ad: NativeAd) =
    remember(ad) { ad.icon?.drawable?.toBitmap()?.asImageBitmap() }

@Composable
private fun BoxScope.NativeAdBadges() {
    NativeAdAttribution(
        Modifier.align(Alignment.TopStart),
        contentColor = SurfaceWhite,
        containerColor = AdBadgesBackgroundColor
    )
    NativeAdChoicesView(Modifier.align(Alignment.TopEnd))
}

@Composable
fun SmallNativeAdView(ad: NativeAd, modifier: Modifier = Modifier) {
    val iconBitmap = rememberIconBitmap(ad)
    NativeAdView(
        nativeAd = ad,
        modifier = modifier
            .fillMaxWidth()
            .height(SMALL_NATIVE_AD_HEIGHT)
            .background(
                color = SurfaceWhite,
                shape = RoundedCornerShape(16.dp)
            ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .padding(top = 10.dp, start = 4.dp)
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                iconBitmap?.let { bmp ->
                    NativeAdIconView(Modifier.size(48.dp)) {
                        Image(bitmap = bmp, contentDescription = null)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    ad.headline?.let {
                        NativeAdHeadlineView {
                            Text(
                                text = it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = Body14SemiBold,
                                color = NativeAdTitleColor
                            )
                        }
                    }
                    ad.body?.let {
                        NativeAdBodyView {
                            Text(
                                text = it,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = Body14Regular,
                                color = NativeAdBodyColor
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                ad.callToAction?.let { ctaText ->
                    // Sử dụng Android Native Button trực tiếp
                    NativeAdCallToActionView(
                        text = ctaText,
                        modifier = Modifier
                            .height(36.dp)
                            .width(88.dp)
                    )
                }
            }
            NativeAdBadges()
        }
    }
}

@Composable
fun MediumNativeAdView(ad: NativeAd, modifier: Modifier = Modifier) {
    val iconBitmap = rememberIconBitmap(ad)
    NativeAdView(
        nativeAd = ad,
        modifier = modifier
            .fillMaxWidth()
            .height(MEDIUM_NATIVE_AD_HEIGHT)
            .background(
                color = SurfaceWhite,
                shape = RoundedCornerShape(16.dp)
            ),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .padding(top = 10.dp, start = 4.dp)
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    iconBitmap?.let { bmp ->
                        NativeAdIconView(Modifier.size(40.dp)) {
                            Image(bitmap = bmp, contentDescription = null)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        ad.headline?.let {
                            NativeAdHeadlineView {
                                Text(
                                    text = it,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = Body16SemiBold,
                                    color = NativeAdTitleColor
                                )
                            }
                        }
                        ad.body?.let {
                            NativeAdBodyView {
                                Text(
                                    text = it,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = Body16Regular,
                                    color = NativeAdBodyColor
                                )
                            }
                        }
                    }
                }

                NativeAdMediaView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 6.dp)
                )

                ad.callToAction?.let { ctaText ->
                    NativeAdCallToActionView(
                        text = ctaText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )
                }
            }
            NativeAdBadges()
        }
    }
}

@Composable
fun FullNativeAdView(ad: NativeAd, modifier: Modifier = Modifier) {
    val iconBitmap = rememberIconBitmap(ad)
    NativeAdView(
        nativeAd = ad,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .padding(top = 10.dp, start = 4.dp)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    iconBitmap?.let { bmp ->
                        NativeAdIconView(Modifier.size(56.dp)) {
                            Image(bitmap = bmp, contentDescription = null)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        ad.headline?.let {
                            NativeAdHeadlineView {
                                Text(
                                    text = it,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = Body16SemiBold,
                                    color = NativeAdTitleColor
                                )
                            }
                        }
                        ad.starRating?.let { NativeAdStarRatingView { Text("Rated $it") } }
                    }
                }

                Spacer(Modifier.height(12.dp))

                NativeAdMediaView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .heightIn(min = 120.dp)
                )

                Spacer(Modifier.height(12.dp))

                ad.body?.let {
                    NativeAdBodyView {
                        Text(
                            text = it,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = Body16Regular,
                            color = NativeAdBodyColor
                        )
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ad.price?.let { NativeAdPriceView(Modifier.padding(end = 8.dp)) { Text(it) } }
                    ad.store?.let { NativeAdStoreView(Modifier.padding(end = 8.dp)) { Text(it) } }
                }

                ad.callToAction?.let { ctaText ->
                    NativeAdCallToActionView(
                        text = ctaText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(44.dp)
                    )
                }
            }
            NativeAdBadges()
        }
    }
}

@Composable
fun NativeAdView(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val nativeAdViewRef = remember { mutableStateOf<NativeAdView?>(null) }
    val mediaViewRef = remember { mutableStateOf<MediaView?>(null) }
    val assetRegistry = remember(nativeAd) { NativeAdAssetRegistry() }

    AndroidView(
        factory = { context ->
            val composeView = ComposeView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            NativeAdView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                addView(composeView)
                nativeAdViewRef.value = this
            }
        },
        modifier = modifier,
        update = { view ->
            val composeView = view.getChildAt(0) as? ComposeView
            composeView?.setContent {
                val registerMediaView: (MediaView?) -> Unit =
                    remember { { mv -> mediaViewRef.value = mv } }
                CompositionLocalProvider(
                    LocalNativeAdView provides view,
                    LocalNativeAd provides nativeAd,
                    LocalMediaViewRegister provides registerMediaView,
                    LocalNativeAdAssetRegistry provides assetRegistry,
                ) {
                    content()
                }
            }
        },
    )
    val currentNativeAd by rememberUpdatedState(nativeAd)
    val currentNativeAdView = nativeAdViewRef.value
    val currentMediaView = mediaViewRef.value
    val assetEpoch = assetRegistry.epoch

    DisposableEffect(currentNativeAd, currentNativeAdView, currentMediaView, assetEpoch) {
        currentNativeAdView?.register(currentNativeAd, currentMediaView)
        onDispose { }
    }
}

/**
 * CTA View tối ưu: Trực tiếp là AppCompatButton/TextView của Android View hệ thống.
 * Tránh việc ComposeView chặn dispatch touch event của AdMob SDK.
 */
@Composable
fun NativeAdCallToActionView(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = PrimaryBlue,
    textColor: Color = SurfaceWhite,
    cornerRadiusDp: Float = 100f
) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val assetRegistry = LocalNativeAdAssetRegistry.current

    val bgArgb = backgroundColor.toArgb()
    val textArgb = textColor.toArgb()

    AndroidView(
        factory = { context ->
            AppCompatButton(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                isAllCaps = false
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)

                // Bo góc và background
                val shapeDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    val density = context.resources.displayMetrics.density
                    cornerRadius = cornerRadiusDp * density
                    setColor(bgArgb)
                }
                background = shapeDrawable
            }
        },
        modifier = modifier,
        update = { button ->
            button.text = text
            button.setTextColor(textArgb)
            nativeAdView.callToActionView = button
            assetRegistry.attach(button)
        }
    )
}

@Composable
fun NativeAdHeadlineView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val assetRegistry = LocalNativeAdAssetRegistry.current
    AndroidView(
        factory = { context -> ComposeView(context) },
        modifier = modifier,
        update = { view ->
            nativeAdView.headlineView = view
            view.setContent(content)
            assetRegistry.attach(view)
        },
    )
}

@Composable
fun NativeAdBodyView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val assetRegistry = LocalNativeAdAssetRegistry.current
    AndroidView(
        factory = { context -> ComposeView(context) },
        modifier = modifier,
        update = { view ->
            nativeAdView.bodyView = view
            view.setContent(content)
            assetRegistry.attach(view)
        },
    )
}

@Composable
fun NativeAdIconView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val assetRegistry = LocalNativeAdAssetRegistry.current
    AndroidView(
        factory = { context -> ComposeView(context) },
        modifier = modifier,
        update = { view ->
            nativeAdView.iconView = view
            view.setContent(content)
            assetRegistry.attach(view)
        },
    )
}

@Composable
fun NativeAdMediaView(modifier: Modifier = Modifier, scaleType: ImageView.ScaleType? = null) {
    val registerMediaView = LocalMediaViewRegister.current
    AndroidView(
        factory = { context -> MediaView(context) },
        update = { view ->
            registerMediaView(view)
            scaleType?.let { type -> view.imageScaleType = type }
        },
        modifier = modifier,
    )
}

@Composable
fun NativeAdChoicesView(modifier: Modifier = Modifier) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val assetRegistry = LocalNativeAdAssetRegistry.current
    AndroidView(
        factory = { context ->
            AdChoicesView(context).apply {
                minimumWidth = 15
                minimumHeight = 15
            }
        },
        modifier = modifier,
        update = { view ->
            nativeAdView.adChoicesView = view
            assetRegistry.attach(view)
        },
    )
}

@Composable
fun NativeAdAdvertiserView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val assetRegistry = LocalNativeAdAssetRegistry.current
    AndroidView(
        factory = { context -> ComposeView(context) },
        modifier = modifier,
        update = { view ->
            nativeAdView.advertiserView = view
            view.setContent(content)
            assetRegistry.attach(view)
        },
    )
}

@Composable
fun NativeAdPriceView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val assetRegistry = LocalNativeAdAssetRegistry.current
    AndroidView(
        factory = { context -> ComposeView(context) },
        modifier = modifier,
        update = { view ->
            nativeAdView.priceView = view
            view.setContent(content)
            assetRegistry.attach(view)
        },
    )
}

@Composable
fun NativeAdStarRatingView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val assetRegistry = LocalNativeAdAssetRegistry.current
    AndroidView(
        factory = { context -> ComposeView(context) },
        modifier = modifier,
        update = { view ->
            nativeAdView.starRatingView = view
            view.setContent(content)
            assetRegistry.attach(view)
        },
    )
}

@Composable
fun NativeAdStoreView(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val nativeAdView = LocalNativeAdView.current ?: throw IllegalStateException("NativeAdView null")
    val assetRegistry = LocalNativeAdAssetRegistry.current
    AndroidView(
        factory = { context -> ComposeView(context) },
        modifier = modifier,
        update = { view ->
            nativeAdView.storeView = view
            view.setContent(content)
            assetRegistry.attach(view)
        },
    )
}

@Composable
fun NativeAdAttribution(
    modifier: Modifier = Modifier,
    text: String = "Ad",
    shape: Shape = MaterialTheme.shapes.extraSmall,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    padding: PaddingValues = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
) {
    Box(
        modifier = modifier
            .background(color = containerColor, shape = shape)
            .padding(padding)
    ) {
        Text(text = text, color = contentColor, style = MaterialTheme.typography.labelSmall)
    }
}

private fun NativeAdView.register(nativeAd: NativeAd, mediaView: MediaView?) {
    post {
        this.registerNativeAd(nativeAd, mediaView)
    }
}