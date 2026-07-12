package tachiyomi.data.entry

import kotlinx.coroutines.flow.Flow
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.PlaybackState
import tachiyomi.domain.entry.repository.PlaybackStateRepository

class PlaybackStateRepositoryImpl(
    private val handler: DatabaseHandler,
) : PlaybackStateRepository {

    override suspend fun getByChapterId(chapterId: Long): PlaybackState? {
        return handler.awaitOneOrNull {
            playback_stateQueries.getByChapterId(
                chapterId,
                PlaybackStateMapper::mapState,
            )
        }
    }

    override fun getByChapterIdAsFlow(chapterId: Long): Flow<PlaybackState?> {
        return handler.subscribeToOneOrNull {
            playback_stateQueries.getByChapterId(
                chapterId,
                PlaybackStateMapper::mapState,
            )
        }
    }

    override fun getByEntryIdAsFlow(entryId: Long): Flow<List<PlaybackState>> {
        return handler.subscribeToList {
            playback_stateQueries.getByEntryId(
                entryId,
                PlaybackStateMapper::mapState,
            )
        }
    }

    override suspend fun upsert(state: PlaybackState) {
        handler.await(inTransaction = true) {
            playback_stateQueries.upsertUpdate(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                completed = state.completed,
                lastWatchedAt = state.lastWatchedAt,
                entryId = state.entryId,
                chapterId = state.chapterId,
            )
            playback_stateQueries.upsertInsert(
                entryId = state.entryId,
                chapterId = state.chapterId,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                completed = state.completed,
                lastWatchedAt = state.lastWatchedAt,
            )
        }
    }

    override suspend fun upsertAndSyncEpisodeState(state: PlaybackState) {
        handler.await(inTransaction = true) {
            playback_stateQueries.upsertUpdate(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                completed = state.completed,
                lastWatchedAt = state.lastWatchedAt,
                entryId = state.entryId,
                chapterId = state.chapterId,
            )
            playback_stateQueries.upsertInsert(
                entryId = state.entryId,
                chapterId = state.chapterId,
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                completed = state.completed,
                lastWatchedAt = state.lastWatchedAt,
            )
            chaptersQueries.update(
                entryId = null,
                url = null,
                name = null,
                scanlator = null,
                read = state.completed,
                bookmark = null,
                lastPageRead = if (state.completed) Long.MAX_VALUE else 1,
                chapterNumber = null,
                sourceOrder = null,
                dateFetch = null,
                dateUpload = null,
                version = null,
                isSyncing = null,
                memo = null,
                chapterId = state.chapterId,
            )
        }
    }
}
