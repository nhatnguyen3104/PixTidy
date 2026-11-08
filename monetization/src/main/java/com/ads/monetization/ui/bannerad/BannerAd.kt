package com.ads.monetization.ui.bannerad

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.androidtech.base.theme.Body18SemiBold
import com.androidtech.base.theme.SurfaceWhite
import com.ads.monetization.adholder.BannerAdHolder

@Preview
@Composable
private fun PreviewBannerAd() {
    BannerAd(
        modifier = Modifier.fillMaxWidth(),
        adHolder = null
    )
}

@Composable
fun BannerAd(
    modifier: Modifier,
    adHolder: BannerAdHolder? = null,
    onReleaseBannerAd: (() -> Unit)? = null
) {
    val isInspectMode = LocalInspectionMode.current

    Column(
        modifier = modifier
    ) {
        if (isInspectMode) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .background(Color.LightGray)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "AdMob BannerAd",
                    style = Body18SemiBold,
                    color = SurfaceWhite
                )
            }
            return
        }

        DisposableEffect(adHolder?.adPlace?.placeName) {
            onDispose { onReleaseBannerAd?.invoke() }
        }

        adHolder?.bannerAd?.let { bannerAd ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { _ ->
                        (bannerAd.parent as? ViewGroup)?.removeView(bannerAd)
                        bannerAd.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        bannerAd.layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        bannerAd
                    },
                    update = { view ->
                        view.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                )
            }
        }
    }
}