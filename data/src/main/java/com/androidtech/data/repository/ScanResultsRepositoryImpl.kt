package com.androidtech.data.repository

import com.androidtech.data.local.database.ScanResultsDao
import com.androidtech.data.model.ScanSummaryEntity
import com.androidtech.data.model.SmartInsightEntity
import com.androidtech.domain.model.media.FastScanSummary
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.SmartInsight
import com.androidtech.domain.model.media.StorageSummary
import com.androidtech.domain.repository.ScanResultsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanResultsRepositoryImpl @Inject constructor(
    private val scanResultsDao: ScanResultsDao,
    private val moshi: Moshi
) : ScanResultsRepository {

    private val storageSummaryAdapter = moshi.adapter(StorageSummary::class.java)
    private val mediaItemListAdapter = moshi.adapter<List<MediaItem>>(
        Types.newParameterizedType(List::class.java, MediaItem::class.java)
    )

    override fun getLatestResults(): Flow<FastScanSummary?> =
        scanResultsDao.getLatestSummary().map { entity ->
            entity?.toDomain()
        }

    override fun getLatestInsights(): Flow<List<SmartInsight>> =
        scanResultsDao.getAllInsights().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveResults(summary: FastScanSummary, insights: List<SmartInsight>) {
        val summaryEntity = summary.toEntity()
        val insightEntities = insights.map { it.toEntity() }
        scanResultsDao.updateScanResults(summaryEntity, insightEntities)
    }

    override suspend fun clearResults() {
        scanResultsDao.clearAllResults()
    }

    override suspend fun updateResultsAfterDeletion(deletedItems: List<MediaItem>) {
        if (deletedItems.isEmpty()) return

        val summaryEntity = scanResultsDao.getLatestSummaryOneShot()
        val currentSummary = summaryEntity?.toDomain() ?: FastScanSummary.EMPTY
        
        val insightEntities = scanResultsDao.getAllInsightsOneShot()
        val currentInsights = insightEntities.map { it.toDomain() }

        val deletedIds = deletedItems.map { it.id }.toSet()
        val totalDeletedBytes = deletedItems.sumOf { it.size }
        val totalDeletedCount = deletedItems.size

        val updatedSummary = currentSummary.copy(
            totalMediaCount = (currentSummary.totalMediaCount - totalDeletedCount).coerceAtLeast(0),
            totalStorageBytes = (currentSummary.totalStorageBytes - totalDeletedBytes).coerceAtLeast(0L),
            duplicatesSizeBytes = (currentSummary.duplicatesSizeBytes - totalDeletedBytes).coerceAtLeast(0L),
            photosSizeBytes = (currentSummary.photosSizeBytes - totalDeletedBytes).coerceAtLeast(0L)
        )

        val updatedInsights = currentInsights.map { insight ->
            val remainingItems = insight.items.filterNot { it.id in deletedIds }
            val removedItemsBytes = insight.items.filter { it.id in deletedIds }.sumOf { it.size }
            
            insight.copy(
                items = remainingItems,
                affectedItemsCount = remainingItems.size,
                reclaimableBytes = (insight.reclaimableBytes - removedItemsBytes).coerceAtLeast(0L)
            )
        }.filter { it.affectedItemsCount > 0 }

        saveResults(updatedSummary, updatedInsights)
    }

    private fun ScanSummaryEntity.toDomain(): FastScanSummary {
        val storageSummary = try {
            storageSummaryJson.let { storageSummaryAdapter.fromJson(it) } ?: StorageSummary.UNKNOWN
        } catch (e: Exception) {
            StorageSummary.UNKNOWN
        }
        return FastScanSummary(
            totalMediaCount = totalMediaCount,
            totalStorageBytes = totalStorageBytes,
            screenshotCount = screenshotCount,
            screenshotBytes = screenshotBytes,
            largeVideoCount = largeVideoCount,
            largeVideoBytes = largeVideoBytes,
            photosSizeBytes = photosSizeBytes,
            videosSizeBytes = videosSizeBytes,
            duplicatesSizeBytes = duplicatesSizeBytes,
            storageSummary = storageSummary
        )
    }

    private fun FastScanSummary.toEntity(): ScanSummaryEntity {
        return ScanSummaryEntity(
            totalMediaCount = totalMediaCount,
            totalStorageBytes = totalStorageBytes,
            screenshotCount = screenshotCount,
            screenshotBytes = screenshotBytes,
            largeVideoCount = largeVideoCount,
            largeVideoBytes = largeVideoBytes,
            photosSizeBytes = photosSizeBytes,
            videosSizeBytes = videosSizeBytes,
            duplicatesSizeBytes = duplicatesSizeBytes,
            storageSummaryJson = storageSummaryAdapter.toJson(storageSummary)
        )
    }

    private fun SmartInsightEntity.toDomain(): SmartInsight {
        val items = try {
            itemsJson.let { mediaItemListAdapter.fromJson(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return SmartInsight(
            id = id,
            title = title,
            description = description,
            type = type,
            reclaimableBytes = reclaimableBytes,
            affectedItemsCount = affectedItemsCount,
            items = items,
            confidenceScore = confidenceScore,
            explanation = explanation
        )
    }

    private fun SmartInsight.toEntity(): SmartInsightEntity {
        return SmartInsightEntity(
            id = id,
            title = title,
            description = description,
            type = type,
            reclaimableBytes = reclaimableBytes,
            affectedItemsCount = affectedItemsCount,
            itemsJson = mediaItemListAdapter.toJson(items),
            confidenceScore = confidenceScore,
            explanation = explanation
        )
    }
}
