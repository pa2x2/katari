package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryProgressState

interface EntryProgressRepository {
    suspend fun get(entryId: Long, contentKey: String, resourceKey: String): EntryProgressState?

    suspend fun getByEntryId(entryId: Long): List<EntryProgressState>

    fun getByEntryIdAsFlow(entryId: Long): Flow<List<EntryProgressState>>

    fun getByChapterIdAsFlow(chapterId: Long): Flow<List<EntryProgressState>>

    suspend fun upsert(state: EntryProgressState)

    suspend fun upsertAndSyncChild(state: EntryProgressState)

    suspend fun merge(state: EntryProgressState): EntryProgressState

    suspend fun mergeAndSyncChild(state: EntryProgressState): EntryProgressState
}
