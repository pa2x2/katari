package mihon.entry.interactions.anime

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mihon.entry.interactions.EntryChildGroupFilterProcessor
import tachiyomi.domain.entry.model.Entry

internal class AnimeChildGroupFilterProcessor : EntryChildGroupFilterProcessor {
    override val type: EntryType = EntryType.ANIME

    override fun supports(entry: Entry): Boolean {
        entry.requireAnime()
        return false
    }

    override fun shouldApplyFilter(entry: Entry): Boolean {
        entry.requireAnime()
        return false
    }

    override fun availableGroupsChanged(entryId: Long): Flow<Unit> {
        return emptyFlow()
    }

    override suspend fun availableGroups(entry: Entry, memberIds: Collection<Long>): Set<String> {
        entry.requireAnime()
        return emptySet()
    }

    override fun excludedGroupsChanged(entryId: Long): Flow<Unit> {
        return emptyFlow()
    }

    override suspend fun excludedGroups(entry: Entry, memberIds: Collection<Long>): Set<String> {
        entry.requireAnime()
        return emptySet()
    }

    override suspend fun setExcludedGroups(entry: Entry, memberIds: Collection<Long>, excluded: Set<String>) {
        entry.requireAnime()
    }
}
