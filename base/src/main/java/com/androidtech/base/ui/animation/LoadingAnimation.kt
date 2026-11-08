package com.androidtech.base.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import com.androidtech.base.theme.PrimaryBlue

@Preview
@Composable
private fun PreviewLoading() {
    LoadingAnimation(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    )
}

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    isLoading: Boolean = true,
    spinnerStrokeDp: Dp = 12.dp
) {
    val rotationDurationMs = 1000
    val sweepDurationMs = 700

    // Primary colour – replace with your theme colour (PrimaryBlue)
    val spinnerColor = PrimaryBlue
    val spinnerSizeDp = 164.dp

    // Faint track behind the arc for a more polished look
    val trackColor = spinnerColor.copy(alpha = 0.12f)

    // Gradient stops for the arc itself
    val arcGradient = Brush.sweepGradient(
        colors = listOf(
            spinnerColor.copy(alpha = 0f),
            spinnerColor.copy(alpha = 0.4f),
            spinnerColor
        )
    )

    AnimatedVisibility(
        visible = isLoading,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val transition = rememberInfiniteTransition(label = "Spinner")

        val rotation by key(rotationDurationMs) {
            transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = rotationDurationMs,
                        easing = LinearEasing
                    ),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )
        }

        // Sweep uses a smoother easing + longer duration for organic feel
        val sweep by key(sweepDurationMs) {
            transition.animateFloat(
                initialValue = 40f,
                targetValue = 300f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = sweepDurationMs,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "sweep"
            )
        }

        // Subtle pulse to breathe life into the whole spinner
        val pulse by transition.animateFloat(
            initialValue = 0.92f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Box(
            modifier = Modifier,
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(spinnerSizeDp)
                    // Soft glow around the spinner
                    .graphicsLayer {
                        scaleX = pulse
                        scaleY = pulse
                    }
            ) {
                val strokedPx = spinnerStrokeDp.toPx()
                val inset = strokedPx / 2f
                val arcSize = Size(size.width - strokedPx, size.height - strokedPx)
                val topLeft = Offset(inset, inset)

                // 1. Faint full-circle track (shows the "path" of the spinner)
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokedPx, cap = StrokeCap.Round)
                )

                // 2. Glow: a wider, translucent copy of the arc beneath the main arc
                rotate(degrees = rotation) {
                    drawArc(
                        brush = arcGradient,
                        startAngle = 0f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokedPx * 1.6f,
                            cap = StrokeCap.Round
                        ),
                        alpha = 0.25f
                    )
                }

                // 3. Main gradient arc on top
                rotate(degrees = rotation) {
                    drawArc(
                        brush = arcGradient,
                        startAngle = 0f,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = strokedPx,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}