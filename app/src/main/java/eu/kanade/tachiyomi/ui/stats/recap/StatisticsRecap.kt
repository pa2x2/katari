package eu.kanade.tachiyomi.ui.stats.recap

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsTopEntry
import java.time.DayOfWeek
import java.time.LocalDate

/** What a shared recap shows for one period: totals, the most-read titles and the period's habits. */
@Immutable
data class StatisticsRecap(
    val totalDurationMillis: Long,
    val completionCount: Long,
    val activeDays: Int,
    /** Longest run of consecutive active days inside the period. */
    val longestStreakDays: Int,
    /** Null when nothing was timed. */
    val busiestWeekday: DayOfWeek?,
    val topTitles: List<StatisticsTopEntry>,
)

internal fun buildStatisticsRecap(snapshot: StatisticsActivitySnapshot, type: EntryType?): StatisticsRecap {
    val durationByDay = snapshot.activity
        .filter { type == null || it.type == type }
        .groupBy { LocalDate.parse(it.localDate) }
        .mapValues { (_, rows) -> rows.sumOf { it.durationMillis } }
        .filterValues { it > 0L }
    var longestStreak = 0
    var currentStreak = 0
    var previous: LocalDate? = null
    durationByDay.keys.sorted().forEach { day ->
        currentStreak = if (previous?.plusDays(1L) == day) currentStreak + 1 else 1
        longestStreak = maxOf(longestStreak, currentStreak)
        previous = day
    }
    return StatisticsRecap(
        totalDurationMillis = durationByDay.values.sum(),
        completionCount = snapshot.completions.filter { type == null || it.type == type }.sumOf { it.count },
        activeDays = durationByDay.size,
        longestStreakDays = longestStreak,
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
