package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange
import java.time.LocalDate
import java.time.YearMonth

internal data class StatsActivityNavigationWindow(
    val startDate: LocalDate?,
    val endDate: LocalDate,
)

internal fun StatsRange.windowEndingOn(
    endDate: LocalDate,
    isLatest: Boolean = false,
    monthAnchorDay: Int = endDate.dayOfMonth,
): StatsActivityWindow = StatsActivityWindow(
    range = this,
    startDate = startLocalDate(endDate),
    endDate = endDate,
    isLatest = isLatest,
    monthAnchorDay = monthAnchorDay,
)

internal fun StatsRange.windowForSelection(
    selection: StatsActivityWindow?,
    today: LocalDate,
): StatsActivityWindow {
    if (this == StatsRange.ALL || selection == null || selection.isLatest) {
        return windowEndingOn(today, isLatest = true)
    }
    val endDate = selection.endDate.coerceAtMost(today)
    return windowEndingOn(endDate, isLatest = endDate == today, monthAnchorDay = selection.monthAnchorDay)
}

internal fun StatsActivityWindow.shiftedByBuckets(bucketCount: Int): StatsActivityWindow {
    if (range == StatsRange.ALL || bucketCount == 0) return this
    val shiftedEnd = when (range) {
        StatsRange.SEVEN_DAYS, StatsRange.THIRTY_DAYS -> endDate.minusDays(bucketCount.toLong())
        StatsRange.ONE_YEAR -> YearMonth.from(endDate).minusMonths(bucketCount.toLong()).let { month ->
            month.atDay(monthAnchorDay.coerceAtMost(month.lengthOfMonth()))
        }
        StatsRange.ALL -> endDate
    }
    return range.windowEndingOn(
        shiftedEnd,
        isLatest = false,
        monthAnchorDay = if (range == StatsRange.ONE_YEAR) monthAnchorDay else shiftedEnd.dayOfMonth,
    )
}

internal fun StatsActivityWindow.clampedTo(
    earliestEndDate: LocalDate?,
    latestEndDate: LocalDate,
): StatsActivityWindow {
    if (range == StatsRange.ALL) return range.windowEndingOn(latestEndDate, isLatest = true)
    val earliest = earliestEndDate ?: latestEndDate
    val clampedEnd = endDate.coerceIn(earliest, latestEndDate)
    return range.windowEndingOn(
        clampedEnd,
        isLatest = clampedEnd == latestEndDate,
        monthAnchorDay = monthAnchorDay,
    )
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
            startDate = requireNotNull(startDate).minusMonths(13L),
            endDate = endDate.plusMonths(13L).coerceAtMost(latestEndDate),
        )
        StatsRange.ALL -> StatsActivityNavigationWindow(startDate, endDate)
    }

private fun LocalDate.coerceIn(minimum: LocalDate, maximum: LocalDate): LocalDate = when {
    isBefore(minimum) -> minimum
    isAfter(maximum) -> maximum
    else -> this
}
