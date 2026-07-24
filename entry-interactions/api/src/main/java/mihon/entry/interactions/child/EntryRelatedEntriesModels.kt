package mihon.entry.interactions

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

sealed interface EntryRelatedEntriesLoadResult {
    data class Loaded(
        val entries: List<Entry>,
        val orientation: EntryItemOrientation,
    ) : EntryRelatedEntriesLoadResult

    data class Unavailable(
        val reason: EntryRelatedEntriesUnavailableReason,
    ) : EntryRelatedEntriesLoadResult
}
