package com.androidtech.domain.model.media

/**
 * Result of a successful cleanup operation.
 */
data class CleanupResult(
    val itemsDeletedCount: Int,
    val bytesReclaimed: Long
)
