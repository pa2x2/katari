package mihon.entry.interactions.book.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.statistics.EntryStatisticsAccent

internal object BookEntryStatisticsProvider : EntryStatisticsProvider {
    override val type = EntryType.BOOK
    override val accent = EntryStatisticsAccent.SAGE
}
