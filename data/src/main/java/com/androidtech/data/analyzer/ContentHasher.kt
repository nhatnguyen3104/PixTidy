package com.androidtech.data.analyzer

import android.content.Context
import android.net.Uri
import com.androidtech.domain.model.media.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import androidx.core.net.toUri

/**
 * SHA-256 over raw file bytes for byte-identical duplicate detection. Streamed via a fixed-size
 * buffer (never loads a whole file into memory) so this is safe even for large video files should
 * a caller choose to hash one.
 */
class ContentHasher @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun sha256(item: MediaItem): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(item.uri.toUri())?.use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(BUFFER_SIZE_BYTES)
                while (true) {
                    val bytesRead = input.read(buffer)
                    if (bytesRead == -1) break
                    digest.update(buffer, 0, bytesRead)
                }
                digest.digest().toHexString()
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    private fun ByteArray.toHexString(): String =
        joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {
        const val BUFFER_SIZE_BYTES = 64 * 1024
    }
}
