package mihon.entry.interactions.manga.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.statistics.EntryStatisticsAccent

internal object MangaEntryStatisticsProvider : EntryStatisticsProvider {
    override val type = EntryType.MANGA
    override val accent = EntryStatisticsAccent.ROSE
}
