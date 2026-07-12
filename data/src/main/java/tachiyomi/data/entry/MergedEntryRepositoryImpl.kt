package tachiyomi.data.entry

import app.cash.sqldelight.async.coroutines.awaitAsList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.data.DatabaseHandler
import tachiyomi.domain.entry.model.EntryMerge
import tachiyomi.domain.entry.repository.MergedEntryRepository

@OptIn(ExperimentalCoroutinesApi::class)
class MergedEntryRepositoryImpl(
    private val handler: DatabaseHandler,
    private val profileProvider: ActiveProfileProvider,
) : MergedEntryRepository {

    override suspend fun getAll(): List<EntryMerge> {
        return handler.awaitList {
            merged_entriesQueries.getAll(profileProvider.activeProfileId, ::mapMerge)
        }
    }

    override fun subscribeAll(): Flow<List<EntryMerge>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                merged_entriesQueries.getAll(profileId, ::mapMerge)
            }
        }
    }

    override suspend fun getGroupByEntryId(entryId: Long): List<EntryMerge> {
        return handler.awaitList {
            merged_entriesQueries.getEntriesByEntryId(profileProvider.activeProfileId, entryId, ::mapMerge)
        }
    }

    override fun subscribeGroupByEntryId(entryId: Long): Flow<List<EntryMerge>> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToList {
                merged_entriesQueries.getEntriesByEntryId(profileId, entryId, ::mapMerge)
            }
        }
    }

    override suspend fun getGroupByTargetId(targetEntryId: Long): List<EntryMerge> {
        return handler.awaitList {
            merged_entriesQueries.getEntriesByTargetId(profileProvider.activeProfileId, targetEntryId, ::mapMerge)
        }
    }

    override suspend fun getTargetId(entryId: Long): Long? {
        return handler.awaitOneOrNull {
            merged_entriesQueries.getTargetIdByEntryId(profileProvider.activeProfileId, entryId)
        }
    }

    override fun subscribeTargetId(entryId: Long): Flow<Long?> {
        return profileProvider.activeProfileIdFlow.flatMapLatest { profileId ->
            handler.subscribeToOneOrNull {
                merged_entriesQueries.getTargetIdByEntryId(profileId, entryId)
            }
        }
    }

    override suspend fun upsertGroup(targetEntryId: Long, orderedEntryIds: List<Long>) {
        require(targetEntryId in orderedEntryIds) { "Target entry must be in orderedEntryIds" }
        require(orderedEntryIds.distinct().size == orderedEntryIds.size) { "Duplicate entry ids in merge group" }

        handler.await(inTransaction = true) {
            val profileId = profileProvider.activeProfileId
            val entries = entriesQueries.getEntryIdsAndTypes(profileId, orderedEntryIds) { id, type -> id to type }
                .awaitAsList()
            validateMergeGroupEntries(orderedEntryIds, entries)

            orderedEntryIds.forEach { entryId ->
                merged_entriesQueries.deleteByEntryId(profileId, entryId)
            }
            merged_entriesQueries.deleteByTargetId(profileId, targetEntryId)
            orderedEntryIds.forEachIndexed { index, entryId ->
                merged_entriesQueries.insert(profileId, targetEntryId, entryId, index.toLong())
            }
        }
    }

    override suspend fun removeMembers(targetEntryId: Long, entryIds: List<Long>) {
        if (entryIds.isEmpty()) return

        handler.await(inTransaction = true) {
            val profileId = profileProvider.activeProfileId
            val existing = merged_entriesQueries.getEntriesByTargetId(profileId, targetEntryId, ::mapMerge)
                .awaitAsList()
            if (existing.isEmpty()) return@await

            val remainingIds = existing.map { it.entryId }.filterNot { it in entryIds }
            if (remainingIds.size <= 1) {
                merged_entriesQueries.deleteByTargetId(profileId, targetEntryId)
                return@await
            }

            val newTargetId = remainingIds.firstOrNull { it == targetEntryId } ?: remainingIds.first()
            merged_entriesQueries.deleteByTargetId(profileId, targetEntryId)
            remainingIds.forEachIndexed { index, entryId ->
                merged_entriesQueries.insert(profileId, newTargetId, entryId, index.toLong())
            }
        }
    }

    override suspend fun deleteGroup(targetEntryId: Long) {
        handler.await {
            merged_entriesQueries.deleteByTargetId(profileProvider.activeProfileId, targetEntryId)
        }
    }

    private fun mapMerge(targetEntryId: Long, entryId: Long, position: Long): EntryMerge {
        return EntryMerge(targetId = targetEntryId, entryId = entryId, position = position)
    }
}

internal fun validateMergeGroupEntries(
    orderedEntryIds: List<Long>,
    entries: List<Pair<Long, String>>,
) {
    require(entries.map { it.first }.toSet() == orderedEntryIds.toSet()) {
        "All merged entries must exist in the active profile"
    }
    require(entries.map { it.second }.distinct().size == 1) {
        "Merged entries must have the same type"
    }
}
