package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange
import java.time.LocalDate

internal data class StatsActivityNavigationWindow(
    val startDate: LocalDate?,
    val endDate: LocalDate,
)

internal fun StatsRange.windowEndingOn(
    endDate: LocalDate,
    isLatest: Boolean = false,
): StatsActivityWindow = StatsActivityWindow(
    range = this,
    startDate = startLocalDate(endDate),
    endDate = endDate,
    isLatest = isLatest,
)

internal fun StatsActivityWindow.shiftedByBuckets(bucketCount: Int): StatsActivityWindow {
    if (range == StatsRange.ALL || bucketCount == 0) return this
    val shiftedEnd = when (range) {
        StatsRange.SEVEN_DAYS, StatsRange.THIRTY_DAYS -> endDate.minusDays(bucketCount.toLong())
        StatsRange.ONE_YEAR -> endDate.minusWeeks(bucketCount.toLong())
        StatsRange.ALL -> endDate
    }
    return range.windowEndingOn(shiftedEnd, isLatest = false)
}

internal fun StatsActivityWindow.clampedTo(
    earliestEndDate: LocalDate?,
    latestEndDate: LocalDate,
): StatsActivityWindow {
    if (range == StatsRange.ALL) return range.windowEndingOn(latestEndDate, isLatest = true)
    val earliest = earliestEndDate ?: latestEndDate
    val clampedEnd = endDate.coerceIn(earliest, latestEndDate)
    return range.windowEndingOn(clampedEnd, isLatest = clampedEnd == latestEndDate)
}

internal fun StatsActivityWindow.navigationWindow(latestEndDate: LocalDate): StatsActivityNavigationWindow =
    when (range) {
        StatsRange.SEVEN_DAYS -> StatsActivityNavigationWindow(
            startDate = requireNotNull(startDate).minusDays(7L),
            endDate = endDate.plusDays(7L).coerceAtMost(latestEndDate),
        )
        StatsRange.THIRTY_DAYS -> StatsActivityNavigationWindow(
            startDate = requireNotNull(startDate).minusDays(30L),
            endDate = endDate.plusDays(30L).coerceAtMost(latestEndDate),
        )
        StatsRange.ONE_YEAR -> StatsActivityNavigationWindow(
            startDate = requireNotNull(startDate).minusWeeks(54L),
            endDate = endDate.plusWeeks(54L).coerceAtMost(latestEndDate),
        )
        StatsRange.ALL -> StatsActivityNavigationWindow(startDate, endDate)
    }

private fun LocalDate.coerceIn(minimum: LocalDate, maximum: LocalDate): LocalDate = when {
    isBefore(minimum) -> minimum
    isAfter(maximum) -> maximum
    else -> this
}
