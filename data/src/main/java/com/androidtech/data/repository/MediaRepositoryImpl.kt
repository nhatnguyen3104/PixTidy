package com.androidtech.data.repository

import com.androidtech.data.source.MediaStoreDataSource
import com.androidtech.data.mapper.inferMediaType
import com.androidtech.domain.model.media.MediaItem
import com.androidtech.domain.model.media.MediaType
import com.androidtech.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [MediaRepository] implementation backed by [MediaStoreDataSource].
 *
 * Images and videos are always queried together and merged, then filtered by the
 * requested [MediaType] client-side — this keeps screenshot classification (which
 * depends on filename heuristics applied to image rows) consistent between the
 * reactive stream and one-shot query paths.
 */
class MediaRepositoryImpl @Inject constructor(
    private val mediaStoreDataSource: MediaStoreDataSource
) : MediaRepository {

    override fun getMediaItems(type: MediaType): Flow<List<MediaItem>> {
        return combine(
            mediaStoreDataSource.observeImages(),
            mediaStoreDataSource.observeVideos()
        ) { images, videos ->
            (images + videos).filter { it.inferMediaType() == type }
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun queryAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaStoreDataSource.queryAll()
    }
}
