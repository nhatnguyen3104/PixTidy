package com.androidtech.domain.model.media

/**
 * Classification of a [MediaItem].
 *
 * SCREENSHOT is intentionally split out from IMAGE because SnapSweep treats
 * screenshots as a distinct cleanup category (they accumulate quickly and are
 * usually low-value, so the UI/analytics layers surface them separately from
 * regular photos even though a screenshot is technically an image file).
 */
enum class MediaType {
    IMAGE,
    VIDEO,
    SCREENSHOT,
    UNKNOWN
}