package eu.kanade.presentation.more.stats.components

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * One calendar column. [days] always holds seven slots starting at the locale's first weekday; slots outside
 * the calendar are null. [startsMonth] labels the column that contains the first day of a month.
 */
internal data class StatisticsReadingCalendarWeek(
    val days: List<LocalDate?>,
    val startsMonth: YearMonth?,
)

internal fun readingCalendarWeeks(
    startDate: LocalDate,
    endDate: LocalDate,
    firstDayOfWeek: DayOfWeek,
): List<StatisticsReadingCalendarWeek> {
    val firstColumn = startDate.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    return generateSequence(firstColumn) { it.plusWeeks(1L) }
        .takeWhile { !it.isAfter(endDate) }
        .map { weekStart ->
            val days = (0L until 7L).map { offset ->
                weekStart.plusDays(offset).takeIf { it in startDate..endDate }
            }
            StatisticsReadingCalendarWeek(
                days = days,
                startsMonth = days.firstOrNull { it?.dayOfMonth == 1 }?.let(YearMonth::from),
            )
        }
        .toList()
}
