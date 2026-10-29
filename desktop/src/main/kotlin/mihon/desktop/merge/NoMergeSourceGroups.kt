package mihon.desktop.merge

import dev.zacsweers.metro.Inject
import mihon.entry.interactions.merge.EntryMergeSourceGroupFeature
import mihon.entry.interactions.merge.EntryMergeSourceGroupResult
import tachiyomi.domain.entry.model.Entry

/**
 * Desktop doesn't host merged entries yet, so the members a source lists for an entry stay entries of their own and
 * none joins it. Stands in until it does.
 */
@Inject
class NoMergeSourceGroups : EntryMergeSourceGroupFeature {
    override suspend fun sync(root: Entry, listed: List<Entry>): EntryMergeSourceGroupResult =
        EntryMergeSourceGroupResult.Applied(joinedEntryIds = emptySet())
}
