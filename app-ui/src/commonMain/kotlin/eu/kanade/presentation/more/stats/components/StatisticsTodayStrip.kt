package eu.kanade.presentation.more.stats.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.presentation.kit.surface.DashboardGoalStrip
import eu.kanade.presentation.more.stats.data.StatsDailyGoal
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Today's time against the daily goal, and how many days in a row the goal was met. */
@Composable
internal fun StatisticsTodayStrip(
    goal: StatsDailyGoal,
    formatDuration: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    DashboardGoalStrip(
        progress = (goal.todayMillis.toFloat() / goal.goalMillis).coerceIn(0f, 1f),
        title = stringResource(MR.strings.statistics_today_time, formatDuration(goal.todayMillis)),
        supporting = if (goal.todayMillis >= goal.goalMillis) {
            stringResource(MR.strings.statistics_goal_met, formatDuration(goal.goalMillis))
        } else {
            stringResource(
                MR.strings.statistics_goal_to_go,
                formatDuration(goal.remainingMillisRoundedUp()),
                formatDuration(goal.goalMillis),
            )
        },
        modifier = modifier,
        figure = pluralStringResource(MR.plurals.day, goal.currentStreakDays, goal.currentStreakDays),
        figureLabel = stringResource(MR.strings.statistics_goal_streak_unit),
    )
}

/**
 * Durations show whole minutes rounded down, so the time left is rounded up for it and today's time to add up to the
 * goal.
 */
private fun StatsDailyGoal.remainingMillisRoundedUp(): Long {
    val remaining = goalMillis - todayMillis
    return (remaining + MINUTE_MILLIS - 1) / MINUTE_MILLIS * MINUTE_MILLIS
}

private const val MINUTE_MILLIS = 60_000L
