package com.androidtech.domain.repository

import com.androidtech.domain.model.media.FastScanSummary
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.SmartInsight
import kotlinx.coroutines.flow.Flow

/**
 * Repository for persisting the cumulative totals from the last successful scan.
 * This allows the Home screen to show immediate "Smart" insights (like duplicate counts)
 * from the previous session instead of resetting to zero on every launch.
 */
interface ScanResultsRepository {
    /**
     * Returns a stream of the last saved scan summary.
     */
    fun getLatestResults(): Flow<FastScanSummary?>

    /**
     * Returns a stream of the smart insights found in the last successful scan.
     */
    fun getLatestInsights(): Flow<List<SmartInsight>>

    /**
     * Persists a new scan summary and its associated insights as the latest.
     */
    suspend fun saveResults(summary: FastScanSummary, insights: List<SmartInsight>)
    
    /**
     * Clears the cached results (e.g. after a full cleanup).
     */
    suspend fun clearResults()

    /**
     * Updates the cached scan summary and smart insights dynamically after items are deleted.
     */
    suspend fun updateResultsAfterDeletion(deletedItems: List<MediaItem>)
}
