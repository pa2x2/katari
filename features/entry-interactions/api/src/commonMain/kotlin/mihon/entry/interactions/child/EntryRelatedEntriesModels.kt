package mihon.entry.interactions.child

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.entry.model.Entry

data class EntryRelatedEntriesContext(
    val entry: Entry,
    val source: UnifiedSource?,
)

sealed interface EntryRelatedEntriesAvailability {
    data class Available(
        val orientation: EntryItemOrientation,
    ) : EntryRelatedEntriesAvailability

    data class Unavailable(
        val reason: EntryRelatedEntriesUnavailableReason,
    ) : EntryRelatedEntriesAvailability
}

enum class EntryRelatedEntriesUnavailableReason {
    SOURCE_MISSING,
    SOURCE_UNSUPPORTED,
}

/** A source's group of related entries, stored as library entries; [title] null for an ungrouped list. */
data class EntryRelatedEntryGroup(
    val title: String?,
    val entries: List<Entry>,
)

sealed interface EntryRelatedEntriesLoadResult {
    /** [groups] holds no empty group, and no entry twice within one group. */
    data class Loaded(
        val groups: List<EntryRelatedEntryGroup>,
        val orientation: EntryItemOrientation,
    ) : EntryRelatedEntriesLoadResult

    data class Unavailable(
        val reason: EntryRelatedEntriesUnavailableReason,
    ) : EntryRelatedEntriesLoadResult
}
