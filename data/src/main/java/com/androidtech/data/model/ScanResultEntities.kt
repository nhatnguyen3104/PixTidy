package com.androidtech.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.androidtech.domain.model.media.SmartInsightType

@Entity(tableName = "scan_summary")
data class ScanSummaryEntity(
    @PrimaryKey val id: Int = 1, // We only keep one latest summary
    @ColumnInfo(name = "total_media_count") val totalMediaCount: Int,
    @ColumnInfo(name = "total_storage_bytes") val totalStorageBytes: Long,
    @ColumnInfo(name = "screenshot_count") val screenshotCount: Int,
    @ColumnInfo(name = "screenshot_bytes") val screenshotBytes: Long,
    @ColumnInfo(name = "large_video_count") val largeVideoCount: Int,
    @ColumnInfo(name = "large_video_bytes") val largeVideoBytes: Long,
    @ColumnInfo(name = "photos_size_bytes") val photosSizeBytes: Long,
    @ColumnInfo(name = "videos_size_bytes") val videosSizeBytes: Long,
    @ColumnInfo(name = "duplicates_size_bytes") val duplicatesSizeBytes: Long,
    @ColumnInfo(name = "storage_summary_json") val storageSummaryJson: String
)

@Entity(tableName = "smart_insights")
data class SmartInsightEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val type: SmartInsightType,
    @ColumnInfo(name = "reclaimable_bytes") val reclaimableBytes: Long,
    @ColumnInfo(name = "affected_items_count") val affectedItemsCount: Int,
    @ColumnInfo(name = "items_json") val itemsJson: String,
    @ColumnInfo(name = "confidence_score") val confidenceScore: Float,
    val explanation: String
)
