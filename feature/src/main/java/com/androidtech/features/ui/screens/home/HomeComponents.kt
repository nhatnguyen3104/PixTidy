package com.androidtech.features.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PaintingStyle.Companion.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.androidtech.base.theme.AppTheme
import com.androidtech.base.theme.DocumentBadge
import com.androidtech.base.theme.StorageBadge
import com.androidtech.base.ui.animation.bounceClick
import com.androidtech.base.R

@Composable
fun StorageRingGauge(
    usedGb: Float,
    totalGb: Float,
    availableGb: Float
) {
    val sweepAngleTarget = (usedGb / totalGb) * 260f
    val animatedSweepAngle by animateFloatAsState(
        targetValue = sweepAngleTarget,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ringAnimation"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(230.dp)
    ) {
        val strokeWidth = 14.dp
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val primaryColor = MaterialTheme.colorScheme.primary

        Canvas(modifier = Modifier
            .fillMaxSize()
            .padding(10.dp)) {
            // Background track ring
            drawArc(
                color = trackColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            // Active progress ring
            drawArc(
                color = primaryColor,
                startAngle = 140f,
                sweepAngle = animatedSweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$usedGb",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = stringResource(R.string.text_capacity),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Text(
                text = stringResource(R.string.text_used_capacity, totalGb.toInt()),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = StorageBadge,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.text_available_capacity, availableGb),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.Black.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/*@Preview(showBackground = true)
@Composable
fun StorageRingGaugePreview(
) {
    AppTheme(
        dynamicColor = false
    ) {
        StorageRingGauge(
            usedGb = 100f,
            totalGb = 200f,
            availableGb = 100f
        )
    }
}*/

@Composable
fun PotentialCleanupCard(potentialGb: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Potential cleanup",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Text(
                text = potentialGb,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

/*@Preview(showBackground = true)
@Composable
fun PotentialCleanupCardPreview() {
    AppTheme(
        dynamicColor = false
    ) {
        PotentialCleanupCard(
            potentialGb = "100 GB"
        )
    }
}*/

@Composable
fun CategoryCard(
    title: String,
    sizeText: String,
    icon: ImageVector,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.bounceClick(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = sizeText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            )
        }
    }
}

/*@Preview(showBackground = true)
@Composable
fun CategoryCardPreview() {
    AppTheme(dynamicColor = false) {
        CategoryCard(
            title = "Documents",
            sizeText = "100 GB",
            icon = Icons.Outlined.AutoAwesome,
            badgeColor = DocumentBadge,
            onClick = {}
        )
    }
}*/
