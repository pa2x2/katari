package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntrySync

interface EntrySyncRepository {

    suspend fun getTrackById(id: Long): EntrySync?

    suspend fun getTracksByEntryId(entryId: Long): List<EntrySync>

    fun getTracksAsFlow(): Flow<List<EntrySync>>

    fun getTracksByEntryIdAsFlow(entryId: Long): Flow<List<EntrySync>>

    suspend fun delete(entryId: Long, syncId: Long)

    suspend fun insert(sync: EntrySync)

    suspend fun insertAll(syncs: List<EntrySync>)
}
