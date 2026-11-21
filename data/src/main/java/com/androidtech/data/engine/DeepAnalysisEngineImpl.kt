package com.androidtech.data.engine

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.androidtech.data.logger.DataLogger
import com.androidtech.data.utils.BitmapHashUtils
import com.androidtech.domain.engine.DeepAnalysisEngine
import com.androidtech.domain.model.media.DocumentAnalysisResult
import com.androidtech.domain.model.media.FrameSample
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.repository.MediaAnalysisCacheRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * Deep-analysis implementations. Requires two additional Gradle dependencies beyond the rest of
 * this module:
 *   implementation("com.google.mlkit:text-recognition:16.0.1")
 *   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1") // for Task.await()
 * MediaMetadataRetriever is part of the Android SDK and needs no extra dependency.
 */
class DeepAnalysisEngineImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaAnalysisCacheRepository: MediaAnalysisCacheRepository
) : DeepAnalysisEngine {

    override suspend fun sampleKeyFrames(item: MediaItem, maxFrames: Int): List<FrameSample> =
        withContext(Dispatchers.Default) {
            if (!item.isVideo) return@withContext emptyList()

            // Check cache first
            val cached = runCatching {
                mediaAnalysisCacheRepository.getValidCache(item.id, item.dateModified)
            }.getOrNull()

            val cachedFrames = cached?.videoFrames
            if (cachedFrames != null) {
                DataLogger.d("sampleKeyFrames: Cache HIT for ${item.name} (id=${item.id}), frames=${cachedFrames.size}")
                return@withContext cachedFrames
            }

            DataLogger.d("sampleKeyFrames: Cache MISS, starting for ${item.name} (id=${item.id})")

            val retriever = MediaMetadataRetriever()
            val samples = try {
                retriever.setDataSource(context, item.uri.toUri())
                val durationMs = item.duration
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                    ?: run {
                        DataLogger.w("sampleKeyFrames: failed to extract duration for ${item.name}")
                        return@withContext emptyList()
                    }

                if (durationMs <= 0L) {
                    DataLogger.w("sampleKeyFrames: duration is 0 for ${item.name}")
                    return@withContext emptyList()
                }

                val frameCount = maxFrames.coerceAtLeast(1)
                val stepMs = durationMs / (frameCount + 1)

                val list = mutableListOf<FrameSample>()
                for (i in 1..frameCount) {
                    val timestampMs = stepMs * i
                    val frameBitmap = try {
                        retriever.getFrameAtTime(
                            timestampMs * 1000, // microseconds
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                        )
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        DataLogger.e(e, "sampleKeyFrames: failed to get frame at $timestampMs ms")
                        null
                    } ?: continue

                    val hash = try {
                        BitmapHashUtils.computeDHash(frameBitmap)
                    } finally {
                        frameBitmap.recycle()
                    }
                    list += FrameSample(timestampMs = timestampMs, phash = hash)
                }
                DataLogger.d("sampleKeyFrames: completed for ${item.name}, found ${list.size} samples")
                list
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                DataLogger.e(e, "sampleKeyFrames: unexpected error for ${item.name}")
                emptyList()
            } finally {
                try {
                    retriever.release()
                } catch (ignored: Exception) {
                    // best-effort cleanup
                }
            }

            if (samples.isNotEmpty()) {
                runCatching {
                    mediaAnalysisCacheRepository.saveVideoFrames(item.id, item.dateModified, samples)
                }
            }

            samples
        }

    override suspend fun analyzeDocument(item: MediaItem): DocumentAnalysisResult =
        withContext(Dispatchers.Default) {
            if (!item.isImage) return@withContext NOT_A_DOCUMENT

            // Check cache first
            val cached = runCatching {
                mediaAnalysisCacheRepository.getValidCache(item.id, item.dateModified)
            }.getOrNull()

            val cachedIsDoc = cached?.isDocument
            if (cachedIsDoc != null) {
                DataLogger.d("analyzeDocument: Cache HIT for ${item.name} (id=${item.id}), isDocument=$cachedIsDoc")
                return@withContext DocumentAnalysisResult(
                    isLikelyDocument = cachedIsDoc,
                    confidence = cached.documentConfidence ?: 0f,
                    extractedTextPreview = cached.ocrPreview
                )
            }

            DataLogger.d("analyzeDocument: Cache MISS, starting OCR for ${item.name} (id=${item.id})")

            val bitmap = try {
                BitmapHashUtils.decodeDownsampled(
                    context.contentResolver,
                    item.uri.toUri(),
                    targetMaxDimension = OCR_TARGET_MAX_DIMENSION
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                DataLogger.e(e, "analyzeDocument: failed to decode bitmap for ${item.name}")
                null
            } ?: return@withContext NOT_A_DOCUMENT

            val result = try {
                val inputImage = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val mlResult = recognizer.process(inputImage).await()

                val textLength = mlResult.text.length
                val blockCount = mlResult.textBlocks.size
                val isDocument = textLength >= MIN_TEXT_LENGTH_FOR_DOCUMENT && blockCount >= MIN_BLOCKS_FOR_DOCUMENT
                val confidence = if (isDocument) {
                    (textLength.toFloat() / DOCUMENT_TEXT_LENGTH_SATURATION).coerceIn(0.5f, 0.95f)
                } else {
                    0f
                }
                val preview = if (isDocument) mlResult.text.take(TEXT_PREVIEW_CHAR_LIMIT) else null

                DataLogger.d("analyzeDocument: completed for ${item.name}, isDocument=$isDocument, textLength=$textLength")

                runCatching {
                    mediaAnalysisCacheRepository.saveDocumentAnalysis(
                        mediaId = item.id,
                        dateModified = item.dateModified,
                        isDocument = isDocument,
                        confidence = confidence,
                        preview = preview
                    )
                }

                DocumentAnalysisResult(
                    isLikelyDocument = isDocument,
                    confidence = confidence,
                    extractedTextPreview = preview
                )
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                DataLogger.e(e, "analyzeDocument: OCR failed for ${item.name}")
                NOT_A_DOCUMENT
            } finally {
                bitmap.recycle()
            }

            result
        }

    private companion object {
        const val OCR_TARGET_MAX_DIMENSION = 1024 // OCR needs more resolution than hashing/blur analysis
        const val MIN_TEXT_LENGTH_FOR_DOCUMENT = 40
        const val MIN_BLOCKS_FOR_DOCUMENT = 2
        const val DOCUMENT_TEXT_LENGTH_SATURATION = 600f
        const val TEXT_PREVIEW_CHAR_LIMIT = 200
        val NOT_A_DOCUMENT = DocumentAnalysisResult(
            isLikelyDocument = false,
            confidence = 0f,
            extractedTextPreview = null
        )
    }
}
