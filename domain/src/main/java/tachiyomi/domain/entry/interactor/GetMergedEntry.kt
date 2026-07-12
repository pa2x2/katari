package tachiyomi.domain.entry.interactor

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.EntryMerge
import tachiyomi.domain.entry.repository.MergedEntryRepository

class GetMergedEntry(
    private val repository: MergedEntryRepository,
) {

    suspend fun awaitAll(): List<EntryMerge> {
        return repository.getAll()
    }

    fun subscribeAll(): Flow<List<EntryMerge>> {
        return repository.subscribeAll()
    }

    suspend fun awaitGroupByEntryId(entryId: Long): List<EntryMerge> {
        return repository.getGroupByEntryId(entryId)
    }

    fun subscribeGroupByEntryId(entryId: Long): Flow<List<EntryMerge>> {
        return repository.subscribeGroupByEntryId(entryId)
    }

    suspend fun awaitGroupByTargetId(targetEntryId: Long): List<EntryMerge> {
        return repository.getGroupByTargetId(targetEntryId)
    }

    suspend fun awaitTargetId(entryId: Long): Long? {
        return repository.getTargetId(entryId)
    }

    fun subscribeTargetId(entryId: Long): Flow<Long?> {
        return repository.subscribeTargetId(entryId)
    }

    suspend fun awaitVisibleTargetId(entryId: Long): Long {
        return repository.getTargetId(entryId) ?: entryId
    }
}
