package eu.kanade.presentation.more.stats.recap.components.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Which days the longest run's calendar shows. A run spanning up to six weeks shows as those weeks in one calendar,
 * padded with weeks around it to at least four rows; a longer one as a small calendar for each month it covers.
 */
internal sealed interface RecapRunCalendarLayout {
    /**
     * @param weeks full weeks, each from the first day of the week.
     * @param labels the months named above a week, by its index: the first week's, and any where a month begins.
     */
    data class Weeks(val weeks: List<List<LocalDate>>, val labels: Map<Int, List<YearMonth>>) : RecapRunCalendarLayout

    data class Months(val months: List<Month>) : RecapRunCalendarLayout {
        /** Two months fit side by side with their day numbers; more go three to a row, their days as dots. */
        val withNumbers: Boolean get() = months.size <= 2
        val columns: Int get() = if (withNumbers) 2 else 3
    }

    /** @param weeks the month's weeks, with null for the days of the months around it. */
    data class Month(val month: YearMonth, val weeks: List<List<LocalDate?>>)
}

internal fun recapRunCalendarLayout(
    run: ClosedRange<LocalDate>,
    periodStart: LocalDate,
    periodEnd: LocalDate,
    firstDayOfWeek: DayOfWeek,
): RecapRunCalendarLayout {
    fun weekOf(date: LocalDate) = date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    var first = weekOf(run.start)
    var last = weekOf(run.endInclusive)
    if (ChronoUnit.WEEKS.between(first, last) >= MAX_WEEKS) {
        val months = generateSequence(YearMonth.from(run.start)) { it.plusMonths(1L) }
            .takeWhile { !it.isAfter(YearMonth.from(run.endInclusive)) }
            .map { month ->
                val weeks = generateSequence(weekOf(month.atDay(1))) { it.plusWeeks(1L) }
                    .takeWhile { !it.isAfter(month.atEndOfMonth()) }
                    .map { start ->
                        List(DAYS_IN_WEEK) { day ->
                            start.plusDays(day.toLong()).takeIf { YearMonth.from(it) == month }
                        }
                    }
                    .toList()
                RecapRunCalendarLayout.Month(month, weeks)
            }
            .toList()
        return RecapRunCalendarLayout.Months(months)
    }

    // Weeks around the run are added alternately before and after it, as long as they reach into the period.
    var addBefore = true
    while (ChronoUnit.WEEKS.between(first, last) + 1 < MIN_WEEKS) {
        val canBefore = !first.minusDays(1L).isBefore(periodStart)
        val canAfter = !last.plusWeeks(1L).isAfter(periodEnd)
        when {
            canBefore && (addBefore || !canAfter) -> first = first.minusWeeks(1L)
            canAfter -> last = last.plusWeeks(1L)
            else -> break
        }
        addBefore = !addBefore
    }
    val weeks = generateSequence(first) { it.plusWeeks(1L) }
        .takeWhile { !it.isAfter(last) }
        .map { start -> List(DAYS_IN_WEEK) { start.plusDays(it.toLong()) } }
        .toList()
    val labels = weeks.withIndex().associate { (index, days) ->
        val begun = days.filter { it.dayOfMonth == 1 }.map(YearMonth::from)
        val months = if (index == 0) (listOf(YearMonth.from(days.first())) + begun).distinct() else begun
        index to months
    }.filterValues { it.isNotEmpty() }
    return RecapRunCalendarLayout.Weeks(weeks, labels)
}

private const val DAYS_IN_WEEK = 7
private const val MIN_WEEKS = 4
private const val MAX_WEEKS = 6
