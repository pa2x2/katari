package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.PlaybackState

interface PlaybackStateRepository {

    suspend fun getByChapterId(chapterId: Long): PlaybackState?

    fun getByChapterIdAsFlow(chapterId: Long): Flow<PlaybackState?>

    fun getByEntryIdAsFlow(entryId: Long): Flow<List<PlaybackState>>

    suspend fun upsert(state: PlaybackState)

    suspend fun upsertAndSyncEpisodeState(state: PlaybackState)
}
