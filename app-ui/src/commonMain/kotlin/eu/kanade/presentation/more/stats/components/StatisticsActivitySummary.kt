package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsDailyGoal
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsType
import mihon.core.platform.PlatformContext
import mihon.core.platform.currentPlatformContext
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ArrowDownward
import mihon.icons.materialsymbols.rounded.ArrowUpward
import mihon.presentation.kit.content.InlineIcon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.layout.DashboardTiles
import mihon.presentation.kit.progress.CircularProgressIndicator
import mihon.presentation.kit.surface.DashboardTile
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Time spent with the change since the previous window, daily average, streak and completions. [activity]
 * is already projected to [selectedType]; [types] are the Overview's types, which the completion tile names.
 *
 * With a daily [goal], the Overview of the latest window shows today's progress in place of the daily average and
 * adds the goal streak; the goal counts every type, so type tabs and earlier windows keep the plain summary.
 */
@Composable
internal fun StatisticsActivitySummaryCards(
    activity: StatsActivity?,
    selectedType: StatsType?,
    types: List<StatsType>,
    goal: StatsDailyGoal?,
    formatDuration: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    val numbers = NumberFormat.getIntegerInstance()
    val dash = "—"
    val isLatest = activity?.window?.isLatest != false
    val shownGoal = goal?.takeIf { selectedType == null && isLatest }
    val completionTypes = selectedType?.let(::listOf) ?: types
    val timeSpent: @Composable (Modifier) -> Unit = { tileModifier ->
        SummaryTile(
            label = stringResource(MR.strings.statistics_time_spent),
            value = activity?.let { formatDuration(it.totalDurationMillis) } ?: dash,
            modifier = tileModifier,
            detail = { activity?.let { PeriodComparison(it, formatDuration) } },
        )
    }
    val todayOrAverage: @Composable (Modifier) -> Unit = { tileModifier ->
        if (shownGoal != null) {
            TodayGoalTile(goal = shownGoal, formatDuration = formatDuration, modifier = tileModifier)
        } else {
            SummaryTile(
                label = stringResource(MR.strings.statistics_daily_average),
                value = activity?.takeIf { it.trackedDayCount > 0 }
                    ?.let { formatDuration(it.totalDurationMillis / it.trackedDayCount) }
                    ?: dash,
                modifier = tileModifier,
                detail = {
                    activity?.takeIf { it.trackedDayCount > 0 }?.let {
                        TileDetail(
                            pluralStringResource(
                                MR.plurals.statistics_days_active_of,
                                it.trackedDayCount,
                                it.activeDays,
                                it.trackedDayCount,
                            ),
                        )
                    }
                },
            )
        }
    }
    val streak: @Composable (Modifier) -> Unit = { tileModifier ->
        SummaryTile(
            label = stringResource(
                if (isLatest) MR.strings.statistics_streak else MR.strings.statistics_ending_streak,
            ),
            value =
            activity?.let { pluralStringResource(MR.plurals.day, it.currentStreakDays, it.currentStreakDays) }
                ?: dash,
            modifier = tileModifier,
            detail = {
                activity?.let {
                    TileDetail(
                        stringResource(
                            MR.strings.statistics_best_streak,
                            pluralStringResource(MR.plurals.day, it.longestStreakDays, it.longestStreakDays),
                        ),
                    )
                }
            },
        )
    }
    val completions: @Composable (Modifier) -> Unit = { tileModifier ->
        SummaryTile(
            label = statisticsCompletionLabel(
                types = completionTypes,
                completionCountByType = activity?.completionCountByType.orEmpty(),
            ),
            value = activity?.completionCount?.let(numbers::format) ?: dash,
            modifier = tileModifier,
            detail = {
                activity?.let {
                    statisticsCompletionDetail(it, completionTypes, formatDuration)
                }?.let { TileDetail(it) }
            },
        )
    }
    val goalStreak: (@Composable (Modifier) -> Unit)? = shownGoal?.let { goal ->
        { tileModifier ->
            SummaryTile(
                label = stringResource(MR.strings.statistics_goal_streak),
                value = pluralStringResource(MR.plurals.day, goal.currentStreakDays, goal.currentStreakDays),
                modifier = tileModifier,
                detail = {
                    if (goal.consideredDays > 0) {
                        TileDetail(
                            pluralStringResource(
                                MR.plurals.statistics_goal_met_days,
                                goal.consideredDays,
                                goal.metDays,
                                goal.consideredDays,
                            ),
                        )
                    }
                },
            )
        }
    }
    DashboardTiles(
        tiles = listOfNotNull(timeSpent, todayOrAverage, streak, completions, goalStreak),
        modifier = modifier,
    )
}

@Composable
private fun TodayGoalTile(goal: StatsDailyGoal, formatDuration: (Long) -> String, modifier: Modifier = Modifier) {
    val fraction = (goal.todayMillis.toFloat() / goal.goalMillis).coerceIn(0f, 1f)
    DashboardTile(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = stringResource(MR.strings.statistics_today), role = TextRole.Supporting)
                Spacer(Modifier.height(KatariTheme.spacing.xs))
                Text(text = formatDuration(goal.todayMillis), role = TextRole.Metric, maxLines = 1)
                Spacer(Modifier.height(KatariTheme.spacing.xxs))
                TileDetail(
                    if (goal.todayMillis >= goal.goalMillis) {
                        stringResource(MR.strings.statistics_goal_met, formatDuration(goal.goalMillis))
                    } else {
                        stringResource(
                            MR.strings.statistics_goal_to_go,
                            formatDuration(goal.remainingMillisRoundedUp()),
                            formatDuration(goal.goalMillis),
                        )
                    },
                )
            }
            CircularProgressIndicator(progress = { fraction }, onContainer = true)
        }
    }
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

@Composable
private fun PeriodComparison(activity: StatsActivity, formatDuration: (Long) -> String) {
    val previousTotal = activity.previousTotalDurationMillis
    val previousWindow = activity.previousWindow
    if (previousTotal == null || previousWindow == null) {
        if (activity.window.range != StatsRange.ALL) {
            TileDetail(stringResource(MR.strings.statistics_no_earlier_period))
        }
        return
    }
    val delta = activity.totalDurationMillis - previousTotal
    val change = if (previousTotal > 0L) {
        "${(abs(delta) * 100.0 / previousTotal).roundToLong()}%"
    } else {
        formatDuration(abs(delta))
    }
    val period = currentPlatformContext().formatCompactWindow(previousWindow)
    val tone = when {
        delta > 0L -> ContentTone.Primary
        delta < 0L -> ContentTone.Error
        else -> ContentTone.Secondary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (delta != 0L) {
            InlineIcon(
                imageVector = if (delta > 0L) {
                    MaterialSymbols.Rounded.ArrowUpward
                } else {
                    MaterialSymbols.Rounded.ArrowDownward
                },
                contentDescription = null,
                role = TextRole.Badge,
                tone = tone,
            )
        }
        TileDetail(stringResource(MR.strings.statistics_change_vs_period, change, period), tone = tone)
    }
}

/** Short localized range such as "Sep 15 – 21"; the year is kept only for yearly windows. */
private fun PlatformContext.formatCompactWindow(window: StatsActivityWindow): String = formatStatisticsDays(
    start = window.startDate ?: window.endDate,
    endInclusive = window.endDate,
    withYear = window.range == StatsRange.ONE_YEAR || window.endDate.year != LocalDate.now().year,
)

@Composable
private fun TileDetail(text: String, tone: ContentTone = ContentTone.Secondary) {
    Text(
        text = text,
        role = TextRole.Badge,
        tone = tone,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun SummaryTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    detail: @Composable () -> Unit = {},
) {
    DashboardTile(modifier = modifier) {
        Text(text = label, role = TextRole.Supporting)
        Spacer(Modifier.height(KatariTheme.spacing.xs))
        Text(text = value, role = TextRole.Metric, maxLines = 1)
        Spacer(Modifier.height(KatariTheme.spacing.xxs))
        detail()
    }
}
