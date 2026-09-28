package eu.kanade.tachiyomi.ui.stats

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import java.time.LocalDate

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

/** Longest run of qualifying days that ends on or before [endDate]. */
internal fun StatisticsActivityTimeline.longestStreakEndingBy(
    endDate: LocalDate,
    type: EntryType? = null,
): Int {
    var longest = 0
    var current = 0
    var previous: LocalDate? = null
    qualifyingDays(type).filterNot(endDate::isBefore).sorted().forEach { day ->
        current = if (previous?.plusDays(1L) == day) current + 1 else 1
        longest = maxOf(longest, current)
        previous = day
    }
    return longest
}

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
