package com.androidtech.features.ui.screens.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidtech.base.theme.AppTheme
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScanProgressGauge(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
        label = "progressAnimation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(260.dp)
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        val innerCircleColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerPoint = Offset(size.width / 2, size.height / 2)
            val outerRadius = size.width / 2 - 12.dp.toPx()
            val middleRadius = outerRadius - 26.dp.toPx()
            val innerRadius = middleRadius - 26.dp.toPx()

            // 1. Vòng tròn mảnh trong cùng (Inner Subtle Ring)
            drawCircle(
                color = innerCircleColor,
                radius = innerRadius,
                center = centerPoint,
                style = Stroke(width = 1.dp.toPx())
            )

            // 2. Vòng tròn mảnh giữa (Middle Subtle Ring)
            drawCircle(
                color = innerCircleColor,
                radius = middleRadius,
                center = centerPoint,
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Vòng nền xám ngoài cùng (Outer Track Ring)
            drawCircle(
                color = trackColor,
                radius = outerRadius,
                center = centerPoint,
                style = Stroke(width = 10.dp.toPx())
            )

            // 4. Vòng tiến trình xanh biển chạy theo % (Active Arc)
            val startAngle = -90f // Bắt đầu từ đỉnh 12 giờ
            val sweepAngle = animatedProgress * 360f

            drawArc(
                color = primaryColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            )

            // 5. Chấm tròn trang trí ở đầu tiến trình (Progress Handle Dot)
            if (animatedProgress > 0f) {
                val endAngleRad = Math.toRadians((startAngle + sweepAngle).toDouble())
                val handleX = centerPoint.x + outerRadius * cos(endAngleRad).toFloat()
                val handleY = centerPoint.y + outerRadius * sin(endAngleRad).toFloat()

                drawCircle(
                    color = primaryColor,
                    radius = 8.dp.toPx(),
                    center = Offset(handleX, handleY)
                )
            }
        }

        // Percentage Text Center
        Text(
            text = "${(animatedProgress * 100).toInt()}%",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.scale(pulseScale)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScanProgressGaugePreview(modifier: Modifier = Modifier) {
    AppTheme( dynamicColor = false) {
        ScanProgressGauge (
            progress = 0.5f,
            modifier = modifier.fillMaxSize()
        )
    }
}