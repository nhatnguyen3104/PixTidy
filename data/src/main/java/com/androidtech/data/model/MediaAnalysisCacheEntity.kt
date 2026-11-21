package com.androidtech.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted, pre-calculated Smart Scan features for a single media item. [dateModified] is the
 * validity key: a row is only trustworthy for as long as the on-disk file's dateModified matches
 * the value stored here (see [MediaAnalysisCacheRepository][com.androidtech.domain.repository.MediaAnalysisCacheRepository]).
 */
@Entity(tableName = "media_analysis_cache")
data class MediaAnalysisCacheEntity(
    @PrimaryKey
    @ColumnInfo(name = "media_id")
    val mediaId: Long,

    @ColumnInfo(name = "date_modified")
    val dateModified: Long,

    @ColumnInfo(name = "phash")
    val pHash: String?,

    @ColumnInfo(name = "blur_score")
    val blurScore: Int,

    @ColumnInfo(name = "is_screenshot")
    val isScreenshot: Boolean,

    @ColumnInfo(name = "video_frames_json")
    val videoFramesJson: String?,

    @ColumnInfo(name = "is_document")
    val isDocument: Boolean?,

    @ColumnInfo(name = "document_confidence")
    val documentConfidence: Float?,

    @ColumnInfo(name = "ocr_preview")
    val ocrPreview: String?,

    @ColumnInfo(name = "last_analyzed")
    val lastAnalyzed: Long
)