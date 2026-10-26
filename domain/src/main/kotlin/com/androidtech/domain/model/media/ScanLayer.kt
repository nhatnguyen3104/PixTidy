package com.androidtech.domain.model.media

/** Identifies which scanning pipeline produced a given [ScanProgress] or [ScanProcess] emission. */
enum class ScanLayer {
    NONE,
    FAST,
    SMART,
    DEEP
}