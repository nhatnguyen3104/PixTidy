package com.androidtech.domain.model.media

enum class ScanStage {
    PREPARING,
    METADATA_SCAN,
    DUPLICATE_DETECTION,
    SIMILARITY_ANALYSIS,
    QUALITY_EVALUATION,
    GENERATING_INSIGHTS
}