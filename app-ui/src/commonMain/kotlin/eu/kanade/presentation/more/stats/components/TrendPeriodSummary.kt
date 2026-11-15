package eu.kanade.presentation.more.stats.components

import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import java.time.temporal.ChronoUnit

internal data class TrendPeriodSummary(
    val totalDurationMillis: Long,
    val durationByType: Map<EntryType, Long>,
    /** Days on or after tracking started; the daily average divides by these. */
    val trackedDays: Long,
    val busiest: StatsTrendPoint?,
) {
    val dailyAverageMillis: Long = if (trackedDays > 0L) totalDurationMillis / trackedDays else 0L
}

internal fun summarizeTrendPeriod(points: List<StatsTrendPoint>): TrendPeriodSummary {
    val tracked = points.filter(StatsTrendPoint::isTracked)
    return TrendPeriodSummary(
        totalDurationMillis = tracked.sumOf(StatsTrendPoint::totalDurationMillis),
        durationByType = tracked.flatMap { it.durationByType.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, durations) -> durations.sum() },
        trackedDays = tracked.sumOf { point ->
            ChronoUnit.DAYS.between(checkNotNull(point.trackedStartDate), point.endDate) + 1L
        },
        busiest = tracked.filter { it.totalDurationMillis > 0L }.maxByOrNull(StatsTrendPoint::totalDurationMillis),
    )
}
