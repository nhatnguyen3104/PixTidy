package com.androidtech.domain.model.media

/** Identifies which scanning pipeline produced a given [ScanProgress] or [ScanProcess] emission. */
enum class ScanLayer {
    FAST_SCAN,
    SMART_SCAN,
    DEEP_SMART_SCAN
}