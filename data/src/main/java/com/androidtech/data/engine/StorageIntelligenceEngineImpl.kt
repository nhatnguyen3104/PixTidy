package com.androidtech.data.engine

import android.content.Context
import android.net.Uri
import com.androidtech.domain.model.media.ImageQualityScore
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.SimilarGroup
import com.androidtech.domain.repository.StorageIntelligenceEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import androidx.core.net.toUri
import javax.inject.Inject

/**
 * [StorageIntelligenceEngine] implementation that bridges [PerceptualHashEngineImpl]
 * (near-duplicate visual similarity) and [ImageQualityAnalyzerImpl] (blur/darkness scoring),
 * and additionally performs exact byte-level duplicate detection via content hashing.
 */
class StorageIntelligenceEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val perceptualHashEngine: PerceptualHashEngineImpl,
    private val imageQualityAnalyzer: ImageQualityAnalyzerImpl
) : StorageIntelligenceEngine {

    override suspend fun detectExactDuplicates(items: List<MediaItem>): List<SimilarGroup> =
        withContext(Dispatchers.IO) {
            val itemsByContentHash = HashMap<String, MutableList<MediaItem>>()
            // Pre-bucket by file size first: two files can only be byte-identical if their
            // sizes match, so this avoids hashing every single file up front.
            val itemsBySize = items.groupBy { it.sizeBytes }
            for ((_, sameSizeItems) in itemsBySize) {
                if (sameSizeItems.size < 2) continue
                for (candidate in sameSizeItems) {
                    val hash = computeContentHash(candidate) ?: continue
                    itemsByContentHash.getOrPut(hash) { mutableListOf() }.add(candidate)
                }
            }

            itemsByContentHash.values
                .filter { it.size > 1 }
                .map { group ->
                    buildGroup(
                        items = group,
                        visualHash = null,
                        similarityPercentage = 100,
                        fallbackReason = "Byte-for-byte identical file content"
                    )
                }
        }

    override suspend fun detectSimilarPhotos(items: List<MediaItem>, threshold: Float): List<SimilarGroup> =
        withContext(Dispatchers.Default) {
            val imageItems = items.filter { it.mimeType.startsWith("image/") }
            val hashedItems = imageItems.mapNotNull { item ->
                perceptualHashEngine.computeDHash(item)?.let { hash -> item to hash }
            }

            val maxDistance = ((1f - threshold) * PerceptualHashEngineImpl.HASH_BIT_LENGTH)
                .toInt()
                .coerceAtLeast(1)

            val visited = BooleanArray(hashedItems.size)
            val groups = mutableListOf<SimilarGroup>()

            for (i in hashedItems.indices) {
                if (visited[i]) continue
                val (_, anchorHash) = hashedItems[i]
                val cluster = mutableListOf(hashedItems[i])
                visited[i] = true

                for (j in (i + 1) until hashedItems.size) {
                    if (visited[j]) continue
                    val distance = PerceptualHashEngineImpl.hammingDistance(anchorHash, hashedItems[j].second)
                    if (distance <= maxDistance) {
                        cluster.add(hashedItems[j])
                        visited[j] = true
                    }
                }

                if (cluster.size > 1) {
                    val averageDistance = cluster.drop(1)
                        .map { PerceptualHashEngineImpl.hammingDistance(anchorHash, it.second) }
                        .average()
                    val similarityPercentage = (100 * (1.0 - averageDistance / PerceptualHashEngineImpl.HASH_BIT_LENGTH))
                        .toInt()
                        .coerceIn(0, 100)

                    groups.add(
                        buildGroup(
                            items = cluster.map { it.first },
                            visualHash = anchorHash,
                            similarityPercentage = similarityPercentage,
                            fallbackReason = "Visually near-identical photos (perceptual hash match)"
                        )
                    )
                }
            }

            groups
        }

    override suspend fun analyzeImageQuality(item: MediaItem): ImageQualityScore =
        imageQualityAnalyzer.analyze(item)

    /**
     * Builds a [SimilarGroup], recommending the member with the highest resolution
     * (and, as a tiebreaker, the largest file size) as the one worth keeping — larger,
     * higher-resolution copies are assumed to be the original/best-quality source.
     */
    private fun buildGroup(
        items: List<MediaItem>,
        visualHash: String?,
        similarityPercentage: Int,
        fallbackReason: String
    ): SimilarGroup {
        val recommended = items.maxByOrNull { candidateRank(it) }
        val reason = if (recommended != null) {
            "Highest resolution (${recommended.width}x${recommended.height}) and largest file size in this group"
        } else {
            fallbackReason
        }

        return SimilarGroup(
            groupId = UUID.randomUUID().toString(),
            visualHash = visualHash,
            similarityPercentage = similarityPercentage,
            totalSizeBytes = items.sumOf { it.sizeBytes },
            items = items,
            recommendedKeepItemId = recommended?.id,
            reasonForRecommendation = reason
        )
    }

    private fun candidateRank(item: MediaItem): Long =
        (item.width.toLong() * item.height.toLong()) * RESOLUTION_WEIGHT + item.sizeBytes

    /**
     * Computes a SHA-256 hash over the file's full byte content to detect true, byte-for-byte
     * duplicates — distinct from [PerceptualHashEngineImpl], which detects visually similar
     * but not necessarily identical files.
     */
    private fun computeContentHash(item: MediaItem): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val opened = context.contentResolver.openInputStream(item.uriString.toUri()) ?: return null
            opened.use { stream ->
                val buffer = ByteArray(BUFFER_SIZE_BYTES)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
        } catch (e: Exception) {
            // Missing file or revoked permission mid-scan; treat as unhashable and skip.
            null
        }
    }

    companion object {
        private const val RESOLUTION_WEIGHT = 1_000_000L
        private const val BUFFER_SIZE_BYTES = 8192
    }
}
