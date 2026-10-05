package tachiyomi.domain.history.model.activity

import eu.kanade.tachiyomi.source.entry.EntryType

/** Which sessions an Activity page lists. */
sealed interface HistoryActivityScope {
    data object All : HistoryActivityScope

    data class Type(val type: EntryType) : HistoryActivityScope

    /** One title, or every member of a merged title. */
    data class Entries(val entryIds: List<Long>) : HistoryActivityScope
}
