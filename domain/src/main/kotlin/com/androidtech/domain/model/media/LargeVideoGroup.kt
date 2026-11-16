package com.androidtech.domain.model.media

data class LargeVideoGroup(
    val groupId: String,
    val videoItems: List<MediaItem>,
    val categoryLabel: String,
    val potentialSavingsBytes: Long
) {
    init {
        require(videoItems.isNotEmpty()) { "LargeVideoGroup '$groupId' must contain at least one item" }
    }

    val selectedVideoItems: List<MediaItem> get() = videoItems.filter { it.isSelected }

    val selectedVideoItemsCount: Int get() = videoItems.count { it.isSelected }

    val totalSelectedSize: Long get() = selectedVideoItems.sumOf { it.size }

    val isGroupSelected: Boolean get() = videoItems.all { it.isSelected }
}
