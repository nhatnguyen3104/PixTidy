package com.androidtech.features.ui.screens.scan

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidtech.base.theme.AppTheme
import com.androidtech.base.ui.animation.bounceClick
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

    // Pulse animation for the percentage text
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Glow effect animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(280.dp)
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val primaryLight = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        val surfaceColor = MaterialTheme.colorScheme.surface

        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = size.minDimension
            val centerPoint = Offset(size.width / 2, size.height / 2)
            val outerRadius = (canvasSize / 2) - 16.dp.toPx()
            val middleRadius = outerRadius - 30.dp.toPx()
            val innerRadius = middleRadius - 28.dp.toPx()

            // 1. Outer shadow/glow ring
            drawCircle(
                color = primaryColor.copy(alpha = glowAlpha * 0.3f),
                radius = outerRadius + 12.dp.toPx(),
                center = centerPoint,
                style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Outer track ring (background)
            drawCircle(
                color = trackColor,
                radius = outerRadius,
                center = centerPoint,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Gradient progress arc with shimmer effect
            val startAngle = -90f
            val sweepAngle = animatedProgress * 360f

            // Progress arc with gradient-like appearance using layered strokes
            if (animatedProgress > 0f) {
                val arcRect = androidx.compose.ui.geometry.Rect(
                    center = centerPoint,
                    radius = outerRadius
                )

                // Main progress arc
                drawArc(
                    color = primaryColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )

                // Secondary thinner arc for depth effect
                drawArc(
                    color = primaryColor.copy(alpha = 0.3f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = arcRect.topLeft,
                    size = arcRect.size,
                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 4. Inner decorative rings
            drawCircle(
                color = primaryLight,
                radius = middleRadius,
                center = centerPoint,
                style = Stroke(width = 1.5.dp.toPx())
            )

            drawCircle(
                color = primaryLight.copy(alpha = 0.5f),
                radius = innerRadius,
                center = centerPoint,
                style = Stroke(width = 1.dp.toPx())
            )

            // 5. Center dot accent
            drawCircle(
                color = primaryColor.copy(alpha = 0.1f),
                radius = innerRadius - 12.dp.toPx(),
                center = centerPoint
            )

            // 6. Progress handle dot with glow
            if (animatedProgress > 0.01f) {
                val endAngleRad = Math.toRadians((startAngle + sweepAngle).toDouble())
                val handleX = centerPoint.x + outerRadius * cos(endAngleRad).toFloat()
                val handleY = centerPoint.y + outerRadius * sin(endAngleRad).toFloat()

                // Glow behind handle
                drawCircle(
                    color = primaryColor.copy(alpha = 0.2f),
                    radius = 18.dp.toPx(),
                    center = Offset(handleX, handleY)
                )

                // Main handle dot
                drawCircle(
                    color = primaryColor,
                    radius = 10.dp.toPx(),
                    center = Offset(handleX, handleY)
                )

                // Inner highlight on handle
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = 4.dp.toPx(),
                    center = Offset(handleX - 2.dp.toPx(), handleY - 2.dp.toPx())
                )
            }

            // 7. Tick marks around the gauge (optional decorative)
            val tickCount = 12
            for (i in 0 until tickCount) {
                val angle = -90f + (i * 360f / tickCount)
                val angleRad = Math.toRadians(angle.toDouble())
                val innerTickRadius = outerRadius - 18.dp.toPx()
                val outerTickRadius = outerRadius - 8.dp.toPx()
                val isActive = (i / 12f) <= animatedProgress

                val x1 = centerPoint.x + innerTickRadius * cos(angleRad).toFloat()
                val y1 = centerPoint.y + innerTickRadius * sin(angleRad).toFloat()
                val x2 = centerPoint.x + outerTickRadius * cos(angleRad).toFloat()
                val y2 = centerPoint.y + outerTickRadius * sin(angleRad).toFloat()

                drawLine(
                    color = if (isActive) primaryColor.copy(alpha = 0.6f) else trackColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Percentage text with enhanced styling
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.sp
                ),
                modifier = Modifier.scale(pulseScale)
            )

            // Subtitle text
            Text(
                text = if (animatedProgress >= 1f) "Complete ✓" else "Scanning...",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
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

@Composable
fun ReportHeroCard(totalCleanableSize: String) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(vertical = 28.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sparkle Icon Badge
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Big Cleanable Size Text
                Text(
                    text = totalCleanableSize,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Subtitle Line 1
                Text(
                    text = "can potentially be cleaned",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        fontSize = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle Line 2
                Text(
                    text = "We found files you may no longer need.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }
    }
}

@Composable
fun ResultCategoryRow(
    category: ScanResultCategory,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon with Circle Badge Background
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(category.badgeBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Category Details (Title & Size)
            Column {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = category.sizeText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )
                )
            }
        }
    }
}

data class ScanResultCategory(
    val title: String,
    val sizeText: String,
    val icon: ImageVector,
    val iconTint: Color,
    val badgeBgColor: Color
)
