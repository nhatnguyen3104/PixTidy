package com.androidtech.domain.usecase.scan

import com.androidtech.domain.model.media.ScanProgress
import com.androidtech.domain.model.media.ScanStage
import com.androidtech.domain.repository.MediaRepository
import com.androidtech.domain.repository.StorageIntelligenceEngine
import com.androidtech.domain.repository.StorageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ScanMediaUseCase @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val storageRepository: StorageRepository,
    private val intelligenceEngine: StorageIntelligenceEngine
) {

    /**
     * Executes the media scan pipeline, emitting [ScanProgress] updates as it moves
     * through metadata collection, duplicate detection, and similarity analysis.
     */
    fun execute(): Flow<ScanProgress> = flow {
        // Stage 1: Preparing
        emit(
            ScanProgress.Scanning(
                stage = ScanStage.PREPARING,
                currentFileName = "",
                processedCount = 0,
                totalCount = 0,
                progressPercentage = 0f
            )
        )

        // Stage 2: Metadata Scan
        emit(
            ScanProgress.Scanning(
                stage = ScanStage.METADATA_SCAN,
                currentFileName = "Reading media library...",
                processedCount = 0,
                totalCount = 0,
                progressPercentage = 10f
            )
        )
        val allMedia = mediaRepository.queryAllMedia()
        
        if (allMedia.isEmpty()) {
            val stats = storageRepository.getStorageStats().first()
            emit(ScanProgress.Completed(stats, emptyList(), emptyList(), 0L))
            return@flow
        }

        // Stage 3: Duplicate Detection (Exact)
        emit(
            ScanProgress.Scanning(
                stage = ScanStage.DUPLICATE_DETECTION,
                currentFileName = "Checking for exact duplicates...",
                processedCount = 0,
                totalCount = allMedia.size,
                progressPercentage = 30f
            )
        )
        val duplicates = intelligenceEngine.detectExactDuplicates(allMedia)

        // Stage 4: Similarity Analysis
        emit(
            ScanProgress.Scanning(
                stage = ScanStage.SIMILARITY_ANALYSIS,
                currentFileName = "Analyzing visual similarity...",
                processedCount = 0,
                totalCount = allMedia.size,
                progressPercentage = 60f
            )
        )
        val similar = intelligenceEngine.detectSimilarPhotos(allMedia)

        // Stage 5: Generating Insights & Completion
        emit(
            ScanProgress.Scanning(
                stage = ScanStage.GENERATING_INSIGHTS,
                currentFileName = "Finalizing results...",
                processedCount = allMedia.size,
                totalCount = allMedia.size,
                progressPercentage = 90f
            )
        )
        
        val stats = storageRepository.getStorageStats().first()
        val totalReclaimable = (duplicates + similar).sumOf { group ->
            // Reclaimable bytes in a group are all items EXCEPT the one recommended to keep.
            group.items
                .filter { it.id != group.recommendedKeepItemId }
                .sumOf { it.sizeBytes }
        }

        emit(
            ScanProgress.Completed(
                stats = stats,
                duplicateGroups = duplicates,
                similarGroups = similar,
                totalReclaimableBytes = totalReclaimable
            )
        )
    }

    /**
     * Signals that any active scan should terminate. Since [execute] is a Flow
     * launched in a CoroutineScope, cancellation is primarily handled by cancelling
     * the job in the ViewModel, but this hook allows for additional cleanup if needed.
     */
    fun cancel() {
        // Cleanup logic if any (e.g. clearing temporary caches)
    }
}
