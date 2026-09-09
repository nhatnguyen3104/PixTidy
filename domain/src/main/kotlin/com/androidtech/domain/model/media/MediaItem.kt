package com.androidtech.domain.model.media

/**
 * Immutable, framework-agnostic representation of a single media file on device.
 *
 * Intentionally holds [uri] and [path] as plain Strings (not android.net.Uri / java.io.File)
 * so this model has zero Android framework dependency and can live in a pure Kotlin module.
 */
data class MediaItem(
    val id: Long,
    val uri: String,
    val path: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val dateAdded: Long,
    val dateModified: Long,
    val width: Int,
    val height: Int,
    val duration: Long? = null,
    val groupId: String? = null,
    val isBestKeep: Boolean = false,
    val isSelected: Boolean = false
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isImage: Boolean get() = mimeType.startsWith("image/")

    /** Aspect ratio guard: MediaStore occasionally reports 0x0 for corrupt/pending entries. */
    val hasValidDimensions: Boolean get() = width > 0 && height > 0
}