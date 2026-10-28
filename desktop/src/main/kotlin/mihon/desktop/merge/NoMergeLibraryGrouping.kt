package mihon.desktop.merge

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryGroupResolution
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolution
import tachiyomi.domain.entry.service.EntryLibraryGroupingResolutionPort

/**
 * Desktop doesn't host merged entries yet, so every library entry stands alone, as an unmerged entry does on Android.
 * Stands in until desktop hosts the merge feature.
 */
@Inject
class NoMergeLibraryGrouping : EntryLibraryGroupingResolutionPort {
    override suspend fun resolveLibraryGrouping(profileId: Long, entries: List<Entry>): EntryLibraryGroupingResolution =
        ungrouped(profileId, entries)

    override fun observeLibraryGrouping(
        profileId: Long,
        entries: Flow<List<Entry>>,
    ): Flow<EntryLibraryGroupingResolution> = entries.map { ungrouped(profileId, it) }

    private fun ungrouped(profileId: Long, entries: List<Entry>): EntryLibraryGroupingResolution =
        EntryLibraryGroupingResolution(profileId, entries.map { EntryLibraryGroupResolution(it, listOf(it)) })
}
