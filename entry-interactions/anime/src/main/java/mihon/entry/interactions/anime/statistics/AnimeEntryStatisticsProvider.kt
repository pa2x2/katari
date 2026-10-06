package mihon.entry.interactions.anime.statistics

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.statistics.EntryStatisticsAccent
import tachiyomi.i18n.*

internal object AnimeEntryStatisticsProvider : EntryStatisticsProvider {
    override val type = EntryType.ANIME
    override val accent = EntryStatisticsAccent.SKY
    override val consumedUnitLabel = MR.strings.statistics_episodes_watched
    override val consumedCountPlural = MR.plurals.statistics_episodes_watched_count
    override val itemPaceCarriesAcrossTitles = true
    override val itemNumberLabel = MR.strings.statistics_recap_episode_number
    override val itemRangeLabel = MR.strings.statistics_recap_episode_range
}
