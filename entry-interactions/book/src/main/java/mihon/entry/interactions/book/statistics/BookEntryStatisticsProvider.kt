package mihon.entry.interactions.book.statistics

import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.statistics.EntryStatisticsAccent
import tachiyomi.i18n.*

internal object BookEntryStatisticsProvider : EntryStatisticsProvider {
    override val accent = EntryStatisticsAccent.SAGE
    override val consumedUnitLabel = MR.strings.statistics_chapters_read
    override val consumedUnitName = MR.strings.statistics_unit_chapters
    override val timePerItemLabel = MR.strings.statistics_time_per_chapter
    override val consumedCountPlural = MR.plurals.statistics_chapters_read_count
    override val itemPaceCarriesAcrossTitles = false
    override val itemNumberLabel = MR.strings.statistics_recap_chapter_number
    override val itemRangeLabel = MR.strings.statistics_recap_chapter_range
}
