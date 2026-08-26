package eu.kanade.tachiyomi.ui.stats

import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import java.time.LocalDate

internal fun StatisticsActivityTimeline.streakEndingOn(
    endDate: LocalDate,
    type: EntryType? = null,
    preserveThroughIncompleteEndDate: Boolean = false,
): Int {
    val qualifyingDays = buildSet {
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

private const val STREAK_DURATION_MILLIS = 60_000L
