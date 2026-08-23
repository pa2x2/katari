package mihon.entry.interactions.anime.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.statistics.EntryStatisticsAccent

internal object AnimeEntryStatisticsProvider : EntryStatisticsProvider {
    override val type = EntryType.ANIME
    override val accent = EntryStatisticsAccent.SKY
}
