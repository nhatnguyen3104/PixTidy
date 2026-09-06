package com.androidtech.data.local

import com.androidtech.domain.model.media.CachedMediaAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-lifetime, in-memory front for [MediaAnalysisCacheEntity] lookups. Room is durable but
 * every read is a disk I/O round trip; within a single Smart Scan (and across back-to-back scans
 * in the same app session) the same media ids are looked up repeatedly, so this avoids re-hitting
 * SQLite for anything already seen this session.
 *
 * Exposed as a [StateFlow] (not just a plain map) so a settings/debug screen could observe cache
 * size live; the scan pipeline itself only needs [get]/[put].
 *
 * Not a replacement for Room: this cache is empty on process start and is never the source of
 * truth — [com.androidtech.data.repository.MediaAnalysisCacheRepositoryImpl] always falls through to
 * Room on a miss here, and populates both layers on write.
 */
@Singleton
class InMemoryAnalysisCache @Inject constructor() {

    private val mutex = Mutex()
    private val _entries = MutableStateFlow<Map<Long, CachedMediaAnalysis>>(emptyMap())

    /** Live view of everything currently cached in memory this session. */
    val entries: StateFlow<Map<Long, CachedMediaAnalysis>> = _entries.asStateFlow()

    fun get(mediaId: Long): CachedMediaAnalysis? = _entries.value[mediaId]

    suspend fun put(analysis: CachedMediaAnalysis) {
        mutex.withLock {
            _entries.value += (analysis.mediaId to analysis)
        }
    }

    suspend fun retainOnly(validMediaIds: Set<Long>) {
        mutex.withLock {
            _entries.value = _entries.value.filterKeys { it in validMediaIds }
        }
    }

    suspend fun clear() {
        mutex.withLock { _entries.value = emptyMap() }
    }
}