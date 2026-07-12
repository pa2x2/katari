package mihon.entry.interactions.manga

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import mihon.entry.interactions.EntryChildGroupFilterDataSource
import mihon.entry.interactions.EntryChildGroupFilterProcessor
import tachiyomi.domain.entry.model.Entry

internal class MangaChildGroupFilterProcessor(
    private val dataSource: EntryChildGroupFilterDataSource,
) : EntryChildGroupFilterProcessor {
    override val type: EntryType = EntryType.MANGA

    override fun supports(entry: Entry): Boolean {
        return entry.type == EntryType.MANGA
    }

    override fun shouldApplyFilter(entry: Entry): Boolean {
        entry.requireManga()
        return true
    }

    override fun availableGroupsChanged(entryId: Long): Flow<Unit> {
        return dataSource.availableGroupsChanged(entryId)
    }

    override suspend fun availableGroups(entry: Entry, memberIds: Collection<Long>): Set<String> {
        entry.requireManga()
        return dataSource.availableGroups(memberIds)
    }

    override fun excludedGroupsChanged(entryId: Long): Flow<Unit> {
        return dataSource.excludedGroupsChanged(entryId)
    }

    override suspend fun excludedGroups(entry: Entry, memberIds: Collection<Long>): Set<String> {
        entry.requireManga()
        return dataSource.excludedGroups(memberIds)
    }

    override suspend fun setExcludedGroups(entry: Entry, memberIds: Collection<Long>, excluded: Set<String>) {
        entry.requireManga()
        dataSource.setExcludedGroups(memberIds, excluded)
    }
}
