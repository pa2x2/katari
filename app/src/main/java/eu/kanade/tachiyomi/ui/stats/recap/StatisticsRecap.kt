package eu.kanade.tachiyomi.ui.stats.recap

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.longestStreakEndingBy
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import tachiyomi.domain.statistics.model.StatisticsTopEntry
import java.time.DayOfWeek
import java.time.LocalDate

/** What a shared recap shows for one period: totals, the most-read titles and the period's habits. */
@Immutable
data class StatisticsRecap(
    val totalDurationMillis: Long,
    val completionCount: Long,
    val activeDays: Int,
    /** Longest streak inside the period, counted as the dashboard counts streaks. */
    val longestStreakDays: Int,
    /** Null when nothing was timed. */
    val busiestWeekday: DayOfWeek?,
    val topTitles: List<StatisticsTopEntry>,
)

/** @param snapshot activity of the period only, so streaks can't reach outside it. */
internal fun buildStatisticsRecap(
    snapshot: StatisticsActivitySnapshot,
    type: EntryType?,
    endDate: LocalDate,
): StatisticsRecap {
    val durationByDay = snapshot.activity
        .filter { type == null || it.type == type }
        .groupBy { LocalDate.parse(it.localDate) }
        .mapValues { (_, rows) -> rows.sumOf { it.durationMillis } }
        .filterValues { it > 0L }
    return StatisticsRecap(
        totalDurationMillis = durationByDay.values.sum(),
        completionCount = snapshot.completions.filter { type == null || it.type == type }.sumOf { it.count },
        activeDays = durationByDay.size,
        longestStreakDays = StatisticsActivityTimeline(snapshot.activity, snapshot.completions)
            .longestStreakEndingBy(endDate, type),
        busiestWeekday = durationByDay.entries
            .groupBy({ it.key.dayOfWeek }, { it.value })
            .maxByOrNull { (_, durations) -> durations.sum() }
            ?.key,
        topTitles = snapshot.topEntries
            .filter { type == null || it.type == type }
            .sortedByDescending(StatisticsTopEntry::durationMillis)
            .take(RECAP_TOP_TITLES),
    )
}

private const val RECAP_TOP_TITLES = 3
