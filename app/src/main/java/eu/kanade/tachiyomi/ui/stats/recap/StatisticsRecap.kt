package eu.kanade.tachiyomi.ui.stats.recap

import androidx.compose.runtime.Immutable
import dev.icerock.moko.resources.PluralsResource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.longestStreakEndingBy
import mihon.entry.interactions.statistics.EntryStatisticsContribution
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import tachiyomi.domain.statistics.model.StatisticsTopEntry
import java.time.DayOfWeek
import java.time.LocalDate

/** What a shared recap shows for one period: totals, the most-read titles and the period's habits. */
@Immutable
data class StatisticsRecap(
    val totalDurationMillis: Long,
    /** Finished items by the types' wording, so types counting the same unit share one count. Never holds zero. */
    val consumedCounts: List<StatisticsRecapConsumedCount>,
    val activeDays: Int,
    /** Longest streak inside the period, counted as the dashboard counts streaks. */
    val longestStreakDays: Int,
    /** Null when nothing was timed. */
    val busiestWeekday: DayOfWeek?,
    val topTitles: List<StatisticsTopEntry>,
)

@Immutable
data class StatisticsRecapConsumedCount(val plural: PluralsResource, val count: Long)

/**
 * @param snapshot activity of the period only, so streaks can't reach outside it.
 * @param contributions the Statistics types, in the order their counts are listed.
 */
internal fun buildStatisticsRecap(
    snapshot: StatisticsActivitySnapshot,
    type: EntryType?,
    endDate: LocalDate,
    contributions: List<EntryStatisticsContribution>,
): StatisticsRecap {
    val durationByDay = snapshot.activity
        .filter { type == null || it.type == type }
        .groupBy { LocalDate.parse(it.localDate) }
        .mapValues { (_, rows) -> rows.sumOf { it.durationMillis } }
        .filterValues { it > 0L }
    return StatisticsRecap(
        totalDurationMillis = durationByDay.values.sum(),
        consumedCounts = contributions
            .filter { type == null || it.type == type }
            .groupBy(EntryStatisticsContribution::consumedCountPlural)
            .mapNotNull { (plural, grouped) ->
                val types = grouped.map(EntryStatisticsContribution::type).toSet()
                snapshot.completions.filter { it.type in types }.sumOf { it.count }
                    .takeIf { it > 0L }
                    ?.let { StatisticsRecapConsumedCount(plural, it) }
            },
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
