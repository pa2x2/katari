package tachiyomi.domain.entry.interactor

import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.repository.MergedEntryRepository

class UpdateMergedEntry(
    private val repository: MergedEntryRepository,
    private val entryRepository: EntryRepository,
) {

    suspend fun awaitMerge(targetEntryId: Long, orderedEntryIds: List<Long>) {
        require(targetEntryId in orderedEntryIds) { "Target entry must be in orderedEntryIds" }
        require(orderedEntryIds.distinct().size == orderedEntryIds.size) { "Duplicate entry ids in merge group" }

        val entries = orderedEntryIds.map { entryId ->
            entryRepository.getEntryById(entryId) ?: error("Entry $entryId does not exist")
        }
        require(entries.map { it.type }.distinct().size == 1) { "Merged entries must have the same type" }

        repository.upsertGroup(targetEntryId, orderedEntryIds)
    }

    suspend fun awaitRemoveMembers(targetEntryId: Long, entryIds: List<Long>) {
        repository.removeMembers(targetEntryId, entryIds)
    }

    suspend fun awaitDeleteGroup(targetEntryId: Long) {
        repository.deleteGroup(targetEntryId)
    }
}
