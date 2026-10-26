package com.androidtech.domain.model.media

data class ScreenshotGroup(
    val groupId: String,
    val screenshotItems: List<MediaItem>,
    val timeLabel: String,
    val potentialSavingsBytes: Long
) {
    init {
        require(screenshotItems.isNotEmpty()) { "ScreenshotGroup '$groupId' must contain at least one item" }
    }

    val selectedScreenshotItems: List<MediaItem> get() = screenshotItems.filter { it.isSelected }

    val selectedScreenshotItemsCount: Int get() = screenshotItems.count {it.isSelected}

    val totalSelectedSize: Long get() = selectedScreenshotItems.sumOf { it.size }

    val isGroupSelected: Boolean get() = screenshotItems.all { it.isSelected }
}