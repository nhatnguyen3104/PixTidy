package com.androidtech.data.analyzer

import android.content.Context
import android.net.Uri
import com.androidtech.data.utils.BitmapHashUtils
import com.androidtech.domain.model.media.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * Pure Kotlin/Android Bitmap dHash implementation — no ML/vision library dependency, which keeps
 * Smart Scan's binary size and CPU cost low. Not suitable for rotation-invariant matching (a
 * photo rotated 90 degrees will not match its original), which is an accepted trade-off for
 * "fast" perceptual hashing; true rotation invariance is left to a future ML-based upgrade.
 */
class FastPerceptualHashAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun computePhash(item: MediaItem): String? = withContext(Dispatchers.Default) {
        try {
            BitmapHashUtils.computeDHashFromUri(context.contentResolver, item.uri.toUri())
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            com.androidtech.data.logger.DataLogger.e(e, "ExactSim error: computePhash failed for id=${item.id}, uri=${item.uri}")
            null
        }
    }
}