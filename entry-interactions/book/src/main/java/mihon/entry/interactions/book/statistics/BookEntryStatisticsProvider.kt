package mihon.entry.interactions.book.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.statistics.EntryStatisticsAccent
import tachiyomi.i18n.MR

internal object BookEntryStatisticsProvider : EntryStatisticsProvider {
    override val type = EntryType.BOOK
    override val accent = EntryStatisticsAccent.SAGE
    override val consumedUnitLabel = MR.strings.statistics_chapters_read
}
