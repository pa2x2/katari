package eu.kanade.tachiyomi.ui.stats.recap.delivery

import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapIndex
import tachiyomi.domain.statistics.recap.StatisticsRecapActivity

/** What a recap's notification tells about its period, counted the same way as the recap itself. */
data class StatisticsRecapHeadline(
    val durationMillis: Long,
    val activeDays: Int,
)

/** Null when nothing was timed, so an empty period gets no notification. */
fun recapHeadline(period: StatisticsRecapPeriod, activity: StatisticsRecapActivity): StatisticsRecapHeadline? {
    val index = StatisticsRecapIndex(period, activity, hiddenEntryIds = emptySet(), contributions = emptyList())
    if (index.durationByDay.isEmpty()) return null
    return StatisticsRecapHeadline(index.totalMillis, index.activeDates.size)
}
