package com.androidtech.data.repository

import com.androidtech.data.local.database.ScanResultsDao
import com.androidtech.data.model.ScanSummaryEntity
import com.androidtech.data.model.SmartInsightEntity
import com.androidtech.domain.model.media.ScanLayer
import com.androidtech.domain.model.media.ScanSummary
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.isScreenshot
import com.androidtech.domain.model.media.isLargeVideo
import com.androidtech.domain.model.media.SmartInsight
import com.androidtech.domain.model.media.SmartInsightType
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

    override fun getLatestResults(): Flow<ScanSummary?> =
        scanResultsDao.getLatestSummary().map { entity ->
            entity?.toDomain()
        }

    override fun getLatestInsights(): Flow<List<SmartInsight>> =
        scanResultsDao.getAllInsights().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun saveResults(summary: ScanSummary, insights: List<SmartInsight>) {
        val currentSummaryEntity = scanResultsDao.getLatestSummaryOneShot()
        val currentSummary = currentSummaryEntity?.toDomain()
        val currentInsightEntities = scanResultsDao.getAllInsightsOneShot()
        val currentInsights = currentInsightEntities.map { it.toDomain() }

        val incomingTypes = insights.map { it.type }.toSet()

        // If previous scan was DEEP and current incoming insights don't contain deep insights (e.g. running a SMART scan),
        // preserve existing DEEP insights (OCR documents & duplicate videos).
        val deepInsightsToPreserve = if (currentSummary?.lastScanLevel == ScanLayer.DEEP) {
            currentInsights.filter {
                (it.type == SmartInsightType.DEEP_OCR_DOCUMENTS || it.type == SmartInsightType.DUPLICATE_VIDEOS) &&
                        it.type !in incomingTypes
            }
        } else {
            emptyList()
        }

        val mergedInsights = insights + deepInsightsToPreserve

        var documentCount = summary.documentCount
        var documentBytes = summary.documentBytes

        deepInsightsToPreserve.forEach { insight ->
            when (insight.type) {
                SmartInsightType.DEEP_OCR_DOCUMENTS -> {
                    documentCount = insight.affectedItemsCount
                    documentBytes = insight.reclaimableBytes
                }
                else -> {}
            }
        }

        val finalScanLevel = if (currentSummary?.lastScanLevel == ScanLayer.DEEP || summary.lastScanLevel == ScanLayer.DEEP) {
            ScanLayer.DEEP
        } else {
            summary.lastScanLevel
        }

        val finalSummary = summary.copy(
            lastScanLevel = finalScanLevel,
            documentCount = if (documentCount > 0) documentCount else (currentSummary?.documentCount ?: 0),
            documentBytes = if (documentBytes > 0) documentBytes else (currentSummary?.documentBytes ?: 0L)
        )

        val summaryEntity = finalSummary.toEntity()
        val insightEntities = mergedInsights.map { it.toEntity() }
        scanResultsDao.updateScanResults(summaryEntity, insightEntities)
    }

    override suspend fun clearResults() {
        scanResultsDao.clearAllResults()
    }

    override suspend fun updateResultsAfterDeletion(deletedItems: List<MediaItem>) {
        if (deletedItems.isEmpty()) return

        val summaryEntity = scanResultsDao.getLatestSummaryOneShot() ?: return
        val currentSummary = summaryEntity.toDomain()
        
        val insightEntities = scanResultsDao.getAllInsightsOneShot()
        val currentInsights = insightEntities.map { it.toDomain() }

        val deletedIds = deletedItems.map { it.id }.toSet()
        val totalDeletedBytes = deletedItems.sumOf { it.size }
        val totalDeletedCount = deletedItems.size

        // Calculate fine-grained reductions
        val screenshotCountRemoved = deletedItems.count { it.isScreenshot }
        val screenshotBytesRemoved = deletedItems.filter { it.isScreenshot }.sumOf { it.size }
        val largeVideoCountRemoved = deletedItems.count { it.isLargeVideo }
        val largeVideoBytesRemoved = deletedItems.filter { it.isLargeVideo }.sumOf { it.size }
        val photoBytesRemoved = deletedItems.filter { it.isImage }.sumOf { it.size }
        val videoBytesRemoved = deletedItems.filter { it.isVideo }.sumOf { it.size }

        var duplicatesRemovedBytes = 0L
        var similarityRemovedBytes = 0L
        var blurryRemovedBytes = 0L
        var blurryCountRemoved = 0
        var documentRemovedBytes = 0L
        var documentCountRemoved = 0

        currentInsights.forEach { insight ->
            val removedInThisInsight = insight.items.filter { it.id in deletedIds }.sumOf { it.size }
            val removedCountInThisInsight = insight.items.count { it.id in deletedIds }
            when (insight.type) {
                SmartInsightType.EXACT_DUPLICATES, SmartInsightType.DUPLICATE_VIDEOS -> {
                    duplicatesRemovedBytes += removedInThisInsight
                }
                SmartInsightType.SIMILAR_PHOTOS -> {
                    similarityRemovedBytes += removedInThisInsight
                }
                SmartInsightType.BLURRY_PHOTOS -> {
                    blurryRemovedBytes += removedInThisInsight
                    blurryCountRemoved += removedCountInThisInsight
                }
                SmartInsightType.DEEP_OCR_DOCUMENTS -> {
                    documentRemovedBytes += removedInThisInsight
                    documentCountRemoved += removedCountInThisInsight
                }
                else -> {}
            }
        }

        val updatedSummary = currentSummary.copy(
            totalMediaCount = (currentSummary.totalMediaCount - totalDeletedCount).coerceAtLeast(0),
            totalStorageBytes = (currentSummary.totalStorageBytes - totalDeletedBytes).coerceAtLeast(0L),
            screenshotCount = (currentSummary.screenshotCount - screenshotCountRemoved).coerceAtLeast(0),
            screenshotBytes = (currentSummary.screenshotBytes - screenshotBytesRemoved).coerceAtLeast(0L),
            largeVideoCount = (currentSummary.largeVideoCount - largeVideoCountRemoved).coerceAtLeast(0),
            largeVideoBytes = (currentSummary.largeVideoBytes - largeVideoBytesRemoved).coerceAtLeast(0L),
            blurryCount = (currentSummary.blurryCount - blurryCountRemoved).coerceAtLeast(0),
            blurrySizeBytes = (currentSummary.blurrySizeBytes - blurryRemovedBytes).coerceAtLeast(0L),
            documentCount = (currentSummary.documentCount - documentCountRemoved).coerceAtLeast(0),
            documentBytes = (currentSummary.documentBytes - documentRemovedBytes).coerceAtLeast(0L),
            photosSizeBytes = (currentSummary.photosSizeBytes - photoBytesRemoved).coerceAtLeast(0L),
            videosSizeBytes = (currentSummary.videosSizeBytes - videoBytesRemoved).coerceAtLeast(0L),
            duplicatesSizeBytes = (currentSummary.duplicatesSizeBytes?.minus(duplicatesRemovedBytes))?.coerceAtLeast(0L),
            similaritySizeBytes = (currentSummary.similaritySizeBytes?.minus(similarityRemovedBytes))?.coerceAtLeast(0L),
            storageSummary = currentSummary.storageSummary.copy(
                usedBytes = (currentSummary.storageSummary.usedBytes - totalDeletedBytes).coerceAtLeast(0L),
                freeBytes = currentSummary.storageSummary.freeBytes + totalDeletedBytes,
                mediaUsedBytes = (currentSummary.storageSummary.mediaUsedBytes - totalDeletedBytes).coerceAtLeast(0L)
            )
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

    private fun ScanSummaryEntity.toDomain(): ScanSummary {
        val storageSummary = try {
            storageSummaryJson.let { storageSummaryAdapter.fromJson(it) } ?: StorageSummary.UNKNOWN
        } catch (e: Exception) {
            StorageSummary.UNKNOWN
        }
        return ScanSummary(
            lastScanLevel = lastScanLevel,
            newItemsCount = newItemsCount,
            totalMediaCount = totalMediaCount,
            totalStorageBytes = totalStorageBytes,
            screenshotCount = screenshotCount,
            screenshotBytes = screenshotBytes,
            largeVideoCount = largeVideoCount,
            largeVideoBytes = largeVideoBytes,
            blurryCount = blurryCount,
            blurrySizeBytes = blurryBytes,
            photosSizeBytes = photosSizeBytes,
            videosSizeBytes = videosSizeBytes,
            duplicatesSizeBytes = duplicatesSizeBytes,
            similaritySizeBytes = similaritySizeBytes,
            documentCount = documentCount,
            documentBytes = documentBytes,
            storageSummary = storageSummary
        )
    }

    private fun ScanSummary.toEntity(): ScanSummaryEntity {
        return ScanSummaryEntity(
            lastScanLevel = lastScanLevel,
            newItemsCount = newItemsCount,
            totalMediaCount = totalMediaCount,
            totalStorageBytes = totalStorageBytes,
            screenshotCount = screenshotCount,
            screenshotBytes = screenshotBytes,
            largeVideoCount = largeVideoCount,
            largeVideoBytes = largeVideoBytes,
            blurryCount = blurryCount,
            blurryBytes = blurrySizeBytes,
            photosSizeBytes = photosSizeBytes,
            videosSizeBytes = videosSizeBytes,
            duplicatesSizeBytes = duplicatesSizeBytes,
            similaritySizeBytes = similaritySizeBytes,
            documentCount = documentCount,
            documentBytes = documentBytes,
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
