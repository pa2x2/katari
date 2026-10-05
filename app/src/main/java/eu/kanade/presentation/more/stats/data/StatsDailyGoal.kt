package eu.kanade.presentation.more.stats.data

/**
 * Progress toward the daily reading goal, always ending today whatever window the dashboard shows.
 *
 * @param currentStreakDays consecutive days the goal was met, ending today, or yesterday while today is still short.
 * @param metDays days the goal was met among the last [consideredDays], which stop at the first day with activity.
 */
data class StatsDailyGoal(
    val goalMillis: Long,
    val todayMillis: Long,
    val currentStreakDays: Int,
    val metDays: Int,
    val consideredDays: Int,
)
