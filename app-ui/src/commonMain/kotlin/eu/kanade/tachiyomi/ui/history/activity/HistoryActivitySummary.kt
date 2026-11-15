package eu.kanade.tachiyomi.ui.history.activity

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.buildActivityRhythm
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsWindowActivity
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Totals for the dates an Activity screen covers. A single day is shown by clock hour; longer ranges by day,
 * or by month once daily bars would be too thin to read.
 */
@Immutable
data class HistoryActivitySummary(
    val totalDurationMillis: Long,
    val durationByType: Map<EntryType, Long>,
    val sessionCount: Long,
    val completionCount: Long,
    val hourlyDurationMillis: List<Long>?,
    val buckets: List<HistoryActivitySummaryBucket>,
    val bucketsAreMonths: Boolean,
)

@Immutable
data class HistoryActivitySummaryBucket(
    val startDate: LocalDate,
    val durationByType: Map<EntryType, Long>,
) {
    val durationMillis: Long = durationByType.values.sum()
}

/** The part of a profile's [StatisticsActivitySnapshot] an Activity screen of every type, or of [type], covers. */
internal fun StatisticsActivitySnapshot.windowActivity(type: EntryType?): StatisticsWindowActivity =
    StatisticsWindowActivity(
        activity = activity.filter { type == null || it.type == type },
        completions = completions.filter { type == null || it.type == type },
        sessionCount = sessions.filter { type == null || it.type == type }.sumOf { it.sessionCount },
        segments = segments.filter { type == null || it.type == type },
    )

internal fun summarizeHistoryActivity(
    window: StatisticsWindowActivity,
    startDate: LocalDate,
    endDate: LocalDate,
): HistoryActivitySummary {
    val byDate = window.activity.groupBy { LocalDate.parse(it.localDate) }
    val byMonth = ChronoUnit.DAYS.between(startDate, endDate) + 1L > MAX_DAILY_BUCKETS
    val buckets = when {
        startDate == endDate -> emptyList()
        byMonth -> generateSequence(YearMonth.from(startDate)) { it.plusMonths(1L) }
            .takeWhile { !it.isAfter(YearMonth.from(endDate)) }
            .map { month ->
                HistoryActivitySummaryBucket(
                    startDate = month.atDay(1),
                    durationByType = byDate.filterKeys { YearMonth.from(it) == month }.values.flatten().byType(),
                )
            }
            .toList()
        else -> generateSequence(startDate) { it.plusDays(1L) }
            .takeWhile { !it.isAfter(endDate) }
            .map { day -> HistoryActivitySummaryBucket(day, byDate[day].orEmpty().byType()) }
            .toList()
    }
    return HistoryActivitySummary(
        totalDurationMillis = window.activity.sumOf { it.durationMillis },
        durationByType = window.activity.byType(),
        sessionCount = window.sessionCount,
        completionCount = window.completions.sumOf { it.count },
        hourlyDurationMillis = if (startDate == endDate) {
            buildActivityRhythm(window.segments, window.activity).hourlyDurationMillis
        } else {
            null
        },
        buckets = buckets,
        bucketsAreMonths = byMonth,
    )
}

private fun List<StatisticsActivityBucket>.byType(): Map<EntryType, Long> =
    groupBy { it.type }.mapValues { (_, rows) -> rows.sumOf { it.durationMillis } }

private const val MAX_DAILY_BUCKETS = 62L
