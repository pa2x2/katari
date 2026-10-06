package eu.kanade.tachiyomi.ui.stats

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun StatisticsActivityTimeline.streakEndingOn(
    endDate: LocalDate,
    type: EntryType? = null,
    preserveThroughIncompleteEndDate: Boolean = false,
): Int {
    val qualifyingDays = qualifyingDays(type)
    var streakDay = when {
        endDate in qualifyingDays -> endDate
        preserveThroughIncompleteEndDate -> endDate.minusDays(1L)
        else -> return 0
    }
    var streak = 0
    while (streakDay in qualifyingDays) {
        streak += 1
        streakDay = streakDay.minusDays(1L)
    }
    return streak
}

/** Length of the longest run of qualifying days that ends on or before [endDate]. */
internal fun StatisticsActivityTimeline.longestStreakEndingBy(
    endDate: LocalDate,
    type: EntryType? = null,
): Int = longestStreakRunEndingBy(endDate, type)?.days()?.toInt() ?: 0

/** The earliest of the longest runs of qualifying days that end on or before [endDate]; null without any. */
internal fun StatisticsActivityTimeline.longestStreakRunEndingBy(
    endDate: LocalDate,
    type: EntryType? = null,
): ClosedRange<LocalDate>? {
    var longest: ClosedRange<LocalDate>? = null
    var current: ClosedRange<LocalDate>? = null
    qualifyingDays(type).filterNot(endDate::isBefore).sorted().forEach { day ->
        val run = current?.takeIf { it.endInclusive.plusDays(1L) == day }?.let { it.start..day } ?: day..day
        if (run.days() > (longest?.days() ?: 0L)) longest = run
        current = run
    }
    return longest
}

/** How many days a streak run covers, both ends included. */
internal fun ClosedRange<LocalDate>.days(): Long = ChronoUnit.DAYS.between(start, endInclusive) + 1L

private fun StatisticsActivityTimeline.qualifyingDays(type: EntryType?): Set<LocalDate> = buildSet {
    activity
        .filter { type == null || it.type == type }
        .groupBy { it.localDate }
        .filterValues { rows -> rows.sumOf { it.durationMillis } >= STREAK_DURATION_MILLIS }
        .keys
        .mapTo(this) { LocalDate.parse(it) }
    completions
        .filter { it.count > 0L && (type == null || it.type == type) }
        .mapTo(this) { LocalDate.parse(it.localDate) }
}

private const val STREAK_DURATION_MILLIS = 60_000L
