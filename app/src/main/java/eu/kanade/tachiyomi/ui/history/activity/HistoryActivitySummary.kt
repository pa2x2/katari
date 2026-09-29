package eu.kanade.tachiyomi.ui.history.activity

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.buildActivityRhythm
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
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
    val durationMillis: Long,
)

internal fun summarizeHistoryActivity(
    snapshot: StatisticsActivitySnapshot,
    type: EntryType?,
    startDate: LocalDate,
    endDate: LocalDate,
): HistoryActivitySummary {
    val activity = snapshot.activity.filter { type == null || it.type == type }
    val byDate = activity.groupBy {
        LocalDate.parse(it.localDate)
    }.mapValues { (_, rows) -> rows.sumOf { it.durationMillis } }
    val byMonth = ChronoUnit.DAYS.between(startDate, endDate) + 1L > MAX_DAILY_BUCKETS
    val buckets = if (startDate == endDate) {
        emptyList()
    } else if (byMonth) {
        generateSequence(YearMonth.from(startDate)) { it.plusMonths(1L) }
            .takeWhile { !it.isAfter(YearMonth.from(endDate)) }
            .map { month ->
                HistoryActivitySummaryBucket(
                    startDate = month.atDay(1),
                    durationMillis = byDate.filterKeys { YearMonth.from(it) == month }.values.sum(),
                )
            }
            .toList()
    } else {
        generateSequence(startDate) { it.plusDays(1L) }
            .takeWhile { !it.isAfter(endDate) }
            .map { day -> HistoryActivitySummaryBucket(day, byDate[day] ?: 0L) }
            .toList()
    }
    return HistoryActivitySummary(
        totalDurationMillis = activity.sumOf { it.durationMillis },
        durationByType = activity.groupBy { it.type }.mapValues { (_, rows) -> rows.sumOf { it.durationMillis } },
        sessionCount = snapshot.sessions.filter { type == null || it.type == type }.sumOf { it.sessionCount },
        completionCount = snapshot.completions.filter { type == null || it.type == type }.sumOf { it.count },
        hourlyDurationMillis = if (startDate == endDate) {
            buildActivityRhythm(snapshot.segments.filter { type == null || it.type == type }, activity)
                .hourlyDurationMillis
        } else {
            null
        },
        buckets = buckets,
        bucketsAreMonths = byMonth,
    )
}

private const val MAX_DAILY_BUCKETS = 62L
