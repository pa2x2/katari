package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsDailyGoal
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import java.time.LocalDate
import java.time.temporal.ChronoUnit

internal fun buildDailyGoal(
    timeline: StatisticsActivityTimeline,
    goalMillis: Long,
    today: LocalDate,
): StatsDailyGoal {
    val byDay = timeline.activity
        .groupBy { LocalDate.parse(it.localDate) }
        .mapValues { (_, rows) -> rows.sumOf { it.durationMillis } }
    fun met(day: LocalDate) = (byDay[day] ?: 0L) >= goalMillis

    var streakDay = if (met(today)) today else today.minusDays(1L)
    var streak = 0
    while (met(streakDay)) {
        streak += 1
        streakDay = streakDay.minusDays(1L)
    }
    val windowStart = today.minusDays(GOAL_HISTORY_DAYS - 1L)
    // Days before the first recorded activity couldn't have met the goal, so they don't count against it.
    val firstActiveDay = byDay.keys.minOrNull() ?: today
    val consideredStart = firstActiveDay.takeIf { it.isAfter(windowStart) } ?: windowStart
    val consideredDays = if (consideredStart.isAfter(today)) 0 else ChronoUnit.DAYS.between(consideredStart, today) + 1L
    return StatsDailyGoal(
        goalMillis = goalMillis,
        todayMillis = byDay[today] ?: 0L,
        currentStreakDays = streak,
        metDays = (0 until consideredDays).count { met(today.minusDays(it)) },
        consideredDays = consideredDays.toInt(),
    )
}

private const val GOAL_HISTORY_DAYS = 30L
