package com.androidtech.features.ui.screens.duplicate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.androidtech.base.theme.AppTheme
import com.androidtech.base.ui.animation.bounceClick
import com.androidtech.base.R
import com.androidtech.base.utils.formatSize
import com.androidtech.domain.model.media.DuplicateGroup
import com.androidtech.domain.model.media.MediaItem

@Composable
fun DuplicateGroupSection(
    group: DuplicateGroup,
    onPhotoToggle: (Long) -> Unit,
    onGroupToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Group Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_duplicate_group, group.originalItem.name),
                    maxLines = 1,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = group.totalSizeDifferenceBytes.formatSize(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                )
                Checkbox(
                    checked = group.isGroupSelected,
                    onCheckedChange = { onGroupToggle(it) },
                    modifier = Modifier
                        .padding(4.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = Color.White.copy(alpha = 0.8f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grid of items: Original (non-selectable) + Duplicates
            val allItems = listOf(group.originalItem) + group.duplicateItems
            val chunkedItems = allItems.chunked(2)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                chunkedItems.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { item ->
                            val isOriginal = item.id == group.originalItem.id
                            Box(modifier = Modifier.weight(1f)) {
                                DuplicatePhotoCard(
                                    duplicateItem = item,
                                    isSelectable = !isOriginal,
                                    onToggle = { if (!isOriginal) onPhotoToggle(item.id) }
                                )
                            }
                        }
                        // Fill empty space if the last row has only one item
                        if (rowItems.size < 2) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DuplicatePhotoCard(
    duplicateItem: MediaItem,
    isSelectable: Boolean = true,
    onToggle: () -> Unit
) {
    val borderColor = if (duplicateItem.isSelected && isSelectable) MaterialTheme.colorScheme.primary else Color.Transparent
    val borderWidth = if (duplicateItem.isSelected && isSelectable) 3.dp else 0.dp

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .then(
                if (isSelectable) Modifier.clickable { onToggle() } else Modifier
            )
    ) {
        // Thumbnail Image
        AsyncImage(
            model = duplicateItem.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top-Left Checkbox (Only if selectable)
        if (isSelectable) {
            Checkbox(
                checked = duplicateItem.isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .padding(4.dp)
                    .align(Alignment.TopStart),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = Color.White.copy(alpha = 0.8f)
                )
            )
        }

        // Bottom-Left "Best Shot / Keep" Star Badge
        if (duplicateItem.isBestKeep) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Star,
                    contentDescription = "Recommended to keep",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

