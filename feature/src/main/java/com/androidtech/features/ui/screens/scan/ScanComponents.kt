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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidtech.base.theme.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidtech.base.theme.AppTheme
import com.androidtech.base.ui.animation.bounceClick
import com.androidtech.domain.model.media.SmartInsightType
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Description
import com.androidtech.base.R
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScanProgressGauge(
    progress: Float,
    modifier: Modifier = Modifier
) {
    // Smoothly animate the progress value
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progressAnimation"
    )

    // Subtle breathing animation for the percentage text
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(240.dp)
    ) {
        val primaryColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            val centerPoint = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension / 2) - strokeWidth

            // 1. Background track
            drawCircle(
                color = trackColor,
                radius = radius,
                center = centerPoint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Progress arc (starts at 12 o'clock)
            val startAngle = -90f
            val sweepAngle = animatedProgress * 360f

            if (sweepAngle > 0f) {
                drawArc(
                    color = primaryColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(centerPoint.x - radius, centerPoint.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Minimal progress handle (soft halo + solid dot)
            if (animatedProgress > 0.005f) {
                val endAngleRad = Math.toRadians((startAngle + sweepAngle).toDouble())
                val handleX = centerPoint.x + radius * cos(endAngleRad).toFloat()
                val handleY = centerPoint.y + radius * sin(endAngleRad).toFloat()
                val handleCenter = Offset(handleX, handleY)

                // Soft halo
                drawCircle(
                    color = primaryColor.copy(alpha = 0.15f),
                    radius = strokeWidth * 1.6f,
                    center = handleCenter
                )

                // Solid handle
                drawCircle(
                    color = primaryColor,
                    radius = strokeWidth * 0.85f,
                    center = handleCenter
                )
            }
        }

        // Center content: percentage + status label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 40.sp,
                    color = primaryColor.copy(alpha = pulseAlpha),
                    letterSpacing = (-1).sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (animatedProgress >= 1f) {
                    stringResource(R.string.text_complete)
                } else {
                    stringResource(R.string.text_scanning)
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    color = onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScanProgressGaugePreview(modifier: Modifier = Modifier) {
    AppTheme( dynamicColor = false, theme = com.androidtech.domain.model.settings.AppTheme.SYSTEM) {
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
                    text = stringResource(R.string.text_scanning_subtitle_1),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        fontSize = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Subtitle Line 2
                Text(
                    text = stringResource(R.string.text_scanning_subtitle_2),
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
fun ScanCategoryCard(
    title: String,
    sizeText: String,
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    comingSoonText: String? = null,
    onClick: () -> Unit
) {
    val activeClick = if (isEnabled) { { onClick() } } else null
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.bounceClick(onClick = activeClick)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            if (!isEnabled && comingSoonText != null) {
                Text(
                    text = comingSoonText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val effectiveBadgeColor = if (isEnabled) badgeColor else badgeColor.copy(alpha = 0.3f)
                val iconColor = if (isEnabled) SurfaceBlack.withAlpha8() else MaterialTheme.colorScheme.onSurfaceVariant.withAlpha5()
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(effectiveBadgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val titleColor = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.withAlpha7()
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = titleColor
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                val descriptionColor = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant.withAlpha7() else MaterialTheme.colorScheme.onSurfaceVariant.withAlpha5()
                val descriptionText = if (isEnabled || comingSoonText == null) sizeText else comingSoonText
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = descriptionColor
                    )
                )
            }
        }
    }
}

val SmartInsightType.icon: ImageVector
    get() = when (this) {
        SmartInsightType.EXACT_DUPLICATES, SmartInsightType.DUPLICATE_VIDEOS -> Icons.Outlined.ContentCopy
        SmartInsightType.SIMILAR_PHOTOS -> Icons.Outlined.Collections
        SmartInsightType.SCREENSHOTS -> Icons.Outlined.Smartphone
        SmartInsightType.LARGE_VIDEOS -> Icons.Outlined.VideoLibrary
        SmartInsightType.BLURRY_PHOTOS -> Icons.Default.BlurOn
        SmartInsightType.DEEP_OCR_DOCUMENTS -> Icons.Default.Description
    }

val SmartInsightType.badgeColor: Color
    get() = when (this) {
        SmartInsightType.EXACT_DUPLICATES, SmartInsightType.DUPLICATE_VIDEOS -> ImageBadge
        SmartInsightType.SIMILAR_PHOTOS -> AudioBadge
        SmartInsightType.SCREENSHOTS -> ArchiveBadge
        SmartInsightType.LARGE_VIDEOS -> VideoBadge
        SmartInsightType.BLURRY_PHOTOS -> FolderBadge
        SmartInsightType.DEEP_OCR_DOCUMENTS -> DocumentBadge
    }

