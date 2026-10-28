package eu.kanade.presentation.more.stats.data

import java.time.DayOfWeek

/** When timed activity happens: local clock hours (0–23) and days of the week. */
data class StatsActivityRhythm(
    val hourlyDurationMillis: List<Long>,
    val weekdayDurationMillis: Map<DayOfWeek, Long>,
) {
    val hasActivity: Boolean = hourlyDurationMillis.any { it > 0L } || weekdayDurationMillis.values.any { it > 0L }

    companion object {
        val EMPTY = StatsActivityRhythm(
            hourlyDurationMillis = List(24) { 0L },
            weekdayDurationMillis = DayOfWeek.entries.associateWith { 0L },
        )
    }
}
