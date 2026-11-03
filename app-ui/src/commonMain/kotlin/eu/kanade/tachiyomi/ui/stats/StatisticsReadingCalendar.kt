package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsReadingCalendar
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

private const val CALENDAR_WEEKS = 52L

/** First day of the calendar: the start of the week that began 52 weeks before today's week. */
internal fun readingCalendarStart(today: LocalDate, locale: Locale): LocalDate = today
    .with(TemporalAdjusters.previousOrSame(WeekFields.of(locale).firstDayOfWeek))
    .minusWeeks(CALENDAR_WEEKS)

internal fun buildReadingCalendar(
    timeline: StatisticsActivityTimeline,
    startDate: LocalDate,
    today: LocalDate,
): StatsReadingCalendar = StatsReadingCalendar(
    startDate = startDate,
    endDate = today,
    durationByDate = timeline.activity
        .filter { it.durationMillis > 0L }
        .groupBy { LocalDate.parse(it.localDate) }
        .filterKeys { it in startDate..today }
        .mapValues { (_, rows) ->
            rows.groupBy { it.type }.mapValues { (_, typeRows) -> typeRows.sumOf { it.durationMillis } }
        },
)
