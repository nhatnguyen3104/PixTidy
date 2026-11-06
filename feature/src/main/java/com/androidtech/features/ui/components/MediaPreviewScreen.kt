package com.androidtech.features.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.androidtech.domain.model.media.MediaItem
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaPreviewScreen(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { mediaList.size }
    var isUIControlsVisible by remember { mutableStateOf(true) }

    // Peek width: how much of the neighbouring item is visible on each side.
    val peekWidth = 32.dp
    val pageSpacing = 12.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Horizontal pager with peek + swipe-to-change
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = peekWidth),
            pageSpacing = pageSpacing
        ) { page ->
            val mediaItem = mediaList[page]
            val isVideo = mediaItem.mimeType.startsWith("video") ||
                    (mediaItem.duration ?: 0L) > 0

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .combinedClickable(
                        onClick = { isUIControlsVisible = !isUIControlsVisible },
                        onDoubleClick = {}
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isVideo) {
                    VideoPlayerComponent(uri = mediaItem.uri)
                } else {
                    // Simple image — no zoom, swipe is handled by the pager
                    AsyncImage(
                        model = mediaItem.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }
        }

        // 2. Top bar: back button + page indicator, with gradient scrim for readability
        AnimatedVisibility(
            visible = isUIControlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Transparent
                            )
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.45f)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        /*// 3. Bottom dots indicator – only when there are multiple items
        AnimatedVisibility(
            visible = isUIControlsVisible && mediaList.size > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(mediaList.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (selected) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) Color.White
                                else Color.White.copy(alpha = 0.35f)
                            )
                    )
                }
            }
        }*/
    }
}

/**
 * Video player backed by Media3 ExoPlayer.
 * - The player is created once per URI and released on dispose.
 * - playWhenReady is false so video doesn't auto-play on swipe.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoPlayerComponent(uri: String) {
    val context = LocalContext.current

    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(Media3Item.fromUri(uri))
            prepare()
            playWhenReady = false
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}