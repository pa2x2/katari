package eu.kanade.tachiyomi.ui.stats.recap.story.pages

import eu.kanade.tachiyomi.ui.stats.buildActivityRhythm
import eu.kanade.tachiyomi.ui.stats.days
import eu.kanade.tachiyomi.ui.stats.longestStreakRunEndingBy
import eu.kanade.tachiyomi.ui.stats.recap.story.MonthHold
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapIndex
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

internal fun StatisticsRecapIndex.monthTitlesPage(): StatisticsRecapPage.MonthTitles? {
    if (durationByMonth.size < MIN_ACTIVE_MONTHS) return null
    val months = periodMonths().map { month -> month to topTitleOf(month) }
    return StatisticsRecapPage.MonthTitles(months, longestHold(months))
}

internal fun StatisticsRecapIndex.monthTimePage(): StatisticsRecapPage.MonthTime? {
    if (durationByMonth.size < MIN_ACTIVE_MONTHS) return null
    val biggest = durationByMonth.maxWith(compareBy({ it.value }, { it.key })).key
    return StatisticsRecapPage.MonthTime(
        months = periodMonths().map { it to (durationByMonth[it] ?: 0L) },
        biggest = biggest,
        biggestTitle = topTitleOf(biggest),
    )
}

internal fun StatisticsRecapIndex.longestRunPage(): StatisticsRecapPage.LongestRun? {
    val run = longestRun() ?: return null
    return StatisticsRecapPage.LongestRun(
        run = run,
        activeDates = activeDates,
        periodStart = period.start,
        periodEnd = period.end,
    )
}

/** The period's longest streak, when it's long enough to tell; the summary card shows the same one. */
internal fun StatisticsRecapIndex.longestRun(): ClosedRange<LocalDate>? =
    timeline.longestStreakRunEndingBy(period.end)?.takeIf { it.days() >= MIN_RUN_DAYS }

internal fun StatisticsRecapIndex.hoursPage(): StatisticsRecapPage.Hours? {
    if (durationByDay.size < MIN_DAYS_FOR_HOURS) return null
    val rhythm = buildActivityRhythm(activitySegments, timeline.activity)
    val hourly = rhythm.hourlyDurationMillis
    val total = hourly.sum().takeIf { it > 0L } ?: return null
    val windowStart = (0 until HOURS_IN_DAY).maxBy { start -> windowDuration(hourly, start) }
    return StatisticsRecapPage.Hours(
        hourlyDurationMillis = hourly,
        topHour = hourly.indices.maxBy { hourly[it] },
        windowStartHour = windowStart,
        windowPercent = (windowDuration(hourly, windowStart) * 100.0 / total).roundToInt(),
        busiestWeekday = rhythm.weekdayDurationMillis.maxBy { it.value }.key,
    )
}

/** The top hour of the period, for the summary card; null when nothing was timed. */
internal fun StatisticsRecapIndex.topHour(): Int? {
    val hourly = buildActivityRhythm(activitySegments, timeline.activity).hourlyDurationMillis
    return hourly.indices.maxByOrNull { hourly[it] }?.takeIf { hourly[it] > 0L }
}

private fun StatisticsRecapIndex.periodMonths(): List<YearMonth> =
    generateSequence(YearMonth.from(period.start)) { it.plusMonths(1L) }
        .takeWhile { !it.isAfter(YearMonth.from(period.end)) }
        .toList()

private fun StatisticsRecapIndex.topTitleOf(month: YearMonth): StatisticsRecapTitle? =
    shownTitlesOf(segments.filter { YearMonth.from(LocalDate.parse(it.localDate)) == month }).firstOrNull()

/** The longest run of two or more consecutive months led by one title; the earliest when runs tie. */
private fun StatisticsRecapIndex.longestHold(months: List<Pair<YearMonth, StatisticsRecapTitle?>>): MonthHold? {
    var best: MonthHold? = null
    var index = 0
    while (index < months.size) {
        val entryId = months[index].second?.entryId
        var end = index
        while (entryId != null && end + 1 < months.size && months[end + 1].second?.entryId == entryId) end++
        val length = end - index + 1
        val bestLength = best?.let { it.from.until(it.to, ChronoUnit.MONTHS) + 1 } ?: 0L
        if (entryId != null && length >= 2 && length > bestLength) {
            best = MonthHold(title(entryId), months[index].first, months[end].first)
        }
        index = end + 1
    }
    return best
}

private fun windowDuration(hourly: List<Long>, start: Int): Long =
    (0 until WINDOW_HOURS).sumOf { hourly[(start + it) % HOURS_IN_DAY] }

private const val MIN_ACTIVE_MONTHS = 6
private const val MIN_RUN_DAYS = 3L
private const val MIN_DAYS_FOR_HOURS = 20
private const val HOURS_IN_DAY = 24
private const val WINDOW_HOURS = 4
