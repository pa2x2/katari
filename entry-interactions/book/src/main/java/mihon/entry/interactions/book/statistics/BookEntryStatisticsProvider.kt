package mihon.entry.interactions.book.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.statistics.EntryStatisticsAccent
import tachiyomi.i18n.*

internal object BookEntryStatisticsProvider : EntryStatisticsProvider {
    override val type = EntryType.BOOK
    override val accent = EntryStatisticsAccent.SAGE
    override val consumedUnitLabel = MR.strings.statistics_chapters_read
    override val consumedCountPlural = MR.plurals.statistics_chapters_read_count
    override val itemPaceCarriesAcrossTitles = false
    override val itemNumberLabel = MR.strings.statistics_recap_chapter_number
    override val itemRangeLabel = MR.strings.statistics_recap_chapter_range
}
