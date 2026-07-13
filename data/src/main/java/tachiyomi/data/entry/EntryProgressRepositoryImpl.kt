package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import tachiyomi.data.Database
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryProgressRepository

class EntryProgressRepositoryImpl(
    private val handler: DatabaseHandler,
) : EntryProgressRepository {
    override suspend fun get(entryId: Long, contentKey: String, resourceKey: String): EntryProgressState? {
        return handler.awaitOneOrNull {
            entry_progress_stateQueries.getByKey(
                entryId = entryId,
                contentKey = contentKey,
                resourceKey = resourceKey,
                mapper = EntryProgressStateMapper::mapState,
            )
        }
    }

    override suspend fun getByEntryId(entryId: Long): List<EntryProgressState> {
        return handler.awaitList {
            entry_progress_stateQueries.getByEntryId(entryId, EntryProgressStateMapper::mapState)
        }
    }

    override fun getByEntryIdAsFlow(entryId: Long): Flow<List<EntryProgressState>> {
        return handler.subscribeToList {
            entry_progress_stateQueries.getByEntryId(entryId, EntryProgressStateMapper::mapState)
        }
    }

    override fun getByChapterIdAsFlow(chapterId: Long): Flow<List<EntryProgressState>> {
        return handler.subscribeToList {
            entry_progress_stateQueries.getByChapterId(chapterId, EntryProgressStateMapper::mapState)
        }
    }

    override suspend fun upsert(state: EntryProgressState) {
        handler.await {
            upsertProgress(state)
        }
    }

    override suspend fun upsertAndSyncChild(state: EntryProgressState) {
        handler.await(inTransaction = true) {
            upsertProgress(state)
            syncChildCompletion(state)
        }
    }

    override suspend fun merge(state: EntryProgressState): EntryProgressState {
        return handler.await(inTransaction = true) {
            mergeProgress(state)
        }
    }

    override suspend fun mergeAndSyncChild(state: EntryProgressState): EntryProgressState {
        return handler.await(inTransaction = true) {
            val merged = mergeProgress(state)
            syncChildCompletion(merged)
            merged
        }
    }

    private suspend fun Database.mergeProgress(incoming: EntryProgressState): EntryProgressState {
        val current = entry_progress_stateQueries.getByKey(
            entryId = incoming.entryId,
            contentKey = incoming.contentKey,
            resourceKey = incoming.resourceKey,
            mapper = EntryProgressStateMapper::mapState,
        ).awaitAsOneOrNull()
        val merged = current?.mergeWith(incoming) ?: incoming
        upsertProgress(merged)
        return merged
    }

    private suspend fun Database.upsertProgress(state: EntryProgressState) {
        entry_progress_stateQueries.upsert(
            entryId = state.entryId,
            chapterId = state.chapterId,
            contentKey = state.contentKey,
            resourceKey = state.resourceKey,
            resourceRevision = state.resourceRevision,
            locatorKind = state.locator.kind,
            position = state.locator.position,
            extent = state.locator.extent,
            progression = state.locator.progression,
            totalProgression = state.locator.totalProgression,
            extensions = state.locator.extensions.toString(),
            completed = state.completed,
            locatorUpdatedAt = state.locatorUpdatedAt,
            completionUpdatedAt = state.completionUpdatedAt,
        )
    }

    private suspend fun Database.syncChildCompletion(state: EntryProgressState) {
        val chapterId = state.chapterId ?: return
        chaptersQueries.update(
            entryId = null,
            url = null,
            name = null,
            scanlator = null,
            read = state.completed,
            bookmark = null,
            lastPageRead = null,
            chapterNumber = null,
            sourceOrder = null,
            dateFetch = null,
            dateUpload = null,
            version = null,
            isSyncing = null,
            memo = null,
            chapterId = chapterId,
        )
    }
}
