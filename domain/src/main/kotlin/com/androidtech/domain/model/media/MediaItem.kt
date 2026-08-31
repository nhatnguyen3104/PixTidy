package com.androidtech.domain.model.media

/**
 * A single image or video file discovered on device storage.
 *
 * This is the atomic unit the rest of the domain layer operates on (duplicate
 * detection, storage breakdowns, scan results, etc). Instances are expected to
 * be immutable snapshots — if the underlying file changes on disk, callers
 * should re-query rather than mutate an existing [MediaItem].
 *
 * @property id Stable identifier for the item. In practice this mirrors the
 *   platform media store's row id, but the domain layer treats it as an
 *   opaque, unique [Long] so it has no framework dependency.
 * @property contentUri Opaque, resolvable URI string (e.g. a `content://` URI)
 *   used by the data layer to open a stream to the file. The domain layer
 *   never parses this — it's a handle, not structured data.
 * @property path Absolute filesystem path, retained for operations (delete,
 *   move) that require direct file access rather than a content resolver.
 * @property displayName User-facing file name, including extension.
 * @property sizeBytes File size on disk, in bytes.
 * @property mimeType Standard MIME type string, e.g. "image/jpeg", "video/mp4".
 * @property dateAddedEpochSec Epoch seconds (UTC) when the file was added to
 *   the media store. Seconds (not millis) to match the platform media store
 *   convention this value is typically sourced from.
 * @property width Pixel width of the image or video frame.
 * @property height Pixel height of the image or video frame.
 * @property durationMs Playback duration in milliseconds. `null` for images;
 *   always non-null for [MediaType.VIDEO] items once fully indexed.
 */
data class MediaItem(
    val id: Long,
    val contentUri: String,
    val path: String,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String,
    val dateAddedEpochSec: Long,
    val width: Int,
    val height: Int,
    val durationMs: Long? = null
) {
    /** Convenience flag; true when this item carries video-specific fields. */
    val isVideo: Boolean get() = durationMs != null
}
