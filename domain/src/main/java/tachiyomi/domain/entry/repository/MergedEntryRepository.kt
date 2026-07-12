package tachiyomi.domain.entry.repository

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryMerge

interface MergedEntryRepository {

    suspend fun getAll(): List<EntryMerge>

    fun subscribeAll(): Flow<List<EntryMerge>>

    suspend fun getGroupByEntryId(entryId: Long): List<EntryMerge>

    fun subscribeGroupByEntryId(entryId: Long): Flow<List<EntryMerge>>

    suspend fun getGroupByTargetId(targetEntryId: Long): List<EntryMerge>

    suspend fun getTargetId(entryId: Long): Long?

    fun subscribeTargetId(entryId: Long): Flow<Long?>

    suspend fun upsertGroup(targetEntryId: Long, orderedEntryIds: List<Long>)

    suspend fun removeMembers(targetEntryId: Long, entryIds: List<Long>)

    suspend fun deleteGroup(targetEntryId: Long)
}
