package mihon.entry.interactions.anime.statistics

import mihon.entry.interactions.anime.state.durationMs
import mihon.entry.interactions.anime.state.positionMs
import mihon.entry.interactions.runtime.EntryStatisticsProvider
import mihon.entry.interactions.statistics.EntryChapterRuntimeReader
import mihon.entry.interactions.statistics.EntryStatisticsAccent
import tachiyomi.domain.statistics.entry.EntryChapterRuntime
import tachiyomi.i18n.*

internal object AnimeEntryStatisticsProvider : EntryStatisticsProvider {
    override val accent = EntryStatisticsAccent.SKY
    override val consumedUnitLabel = MR.strings.statistics_episodes_watched
    override val consumedUnitName = MR.strings.statistics_unit_episodes
    override val timePerItemLabel = MR.strings.statistics_time_per_episode
    override val consumedCountPlural = MR.plurals.statistics_episodes_watched_count
    override val chapterRuntime = EntryChapterRuntimeReader { state ->
        state.chapterId?.let { chapterId ->
            state.durationMs.takeIf { it > 0L }?.let { length ->
                EntryChapterRuntime(chapterId = chapterId, lengthMillis = length, playedMillis = state.positionMs)
            }
        }
    }
    override val itemNumberLabel = MR.strings.statistics_recap_episode_number
    override val itemRangeLabel = MR.strings.statistics_recap_episode_range
}
