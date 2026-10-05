package eu.kanade.presentation.more.stats.components

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsDailyGoal
import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Time spent with the change since the previous window, daily average, streak and completions. [activity]
 * is already projected to [selectedType]; [types] supplies per-type labels for the Overview breakdown.
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
) {
    val numbers = NumberFormat.getIntegerInstance()
    val dash = "—"
    val isLatest = activity?.window?.isLatest != false
    val shownGoal = goal?.takeIf { selectedType == null && isLatest }
    val completionBreakdown = if (selectedType == null && activity != null) {
        types.mapNotNull { type ->
            activity.completionCountByType[type.type]?.takeIf { it > 0L }?.let { count ->
                "${stringResource(type.displayName)} ${numbers.format(count)}"
            }
        }.joinToString(" · ")
    } else {
        ""
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryTile(
                label = stringResource(MR.strings.statistics_time_spent),
                value = activity?.let { formatDuration(it.totalDurationMillis) } ?: dash,
                modifier = Modifier.weight(1f),
                detail = { activity?.let { PeriodComparison(it, formatDuration) } },
            )
            if (shownGoal != null) {
                TodayGoalTile(goal = shownGoal, formatDuration = formatDuration, modifier = Modifier.weight(1f))
            } else {
                SummaryTile(
                    label = stringResource(MR.strings.statistics_daily_average),
                    value = activity?.takeIf { it.trackedDayCount > 0 }
                        ?.let { formatDuration(it.totalDurationMillis / it.trackedDayCount) }
                        ?: dash,
                    modifier = Modifier.weight(1f),
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
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryTile(
                label = stringResource(
                    if (isLatest) MR.strings.statistics_streak else MR.strings.statistics_ending_streak,
                ),
                value =
                activity?.let { pluralStringResource(MR.plurals.day, it.currentStreakDays, it.currentStreakDays) }
                    ?: dash,
                modifier = Modifier.weight(1f),
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
            SummaryTile(
                label = selectedType?.let { stringResource(it.consumedUnitLabel) }
                    ?: stringResource(MR.strings.statistics_completed),
                value = activity?.completionCount?.let(numbers::format) ?: dash,
                modifier = Modifier.weight(1f),
                detail = { if (completionBreakdown.isNotEmpty()) TileDetail(completionBreakdown) },
            )
        }
        if (shownGoal != null) {
            SummaryTile(
                label = stringResource(MR.strings.statistics_goal_streak),
                value = pluralStringResource(MR.plurals.day, shownGoal.currentStreakDays, shownGoal.currentStreakDays),
                modifier = Modifier.fillMaxWidth(),
                detail = {
                    if (shownGoal.consideredDays > 0) {
                        TileDetail(
                            pluralStringResource(
                                MR.plurals.statistics_goal_met_days,
                                shownGoal.consideredDays,
                                shownGoal.metDays,
                                shownGoal.consideredDays,
                            ),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun TodayGoalTile(goal: StatsDailyGoal, formatDuration: (Long) -> String, modifier: Modifier = Modifier) {
    val fraction = (goal.todayMillis.toFloat() / goal.goalMillis).coerceIn(0f, 1f)
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(MR.strings.statistics_today),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatDuration(goal.todayMillis),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                TileDetail(
                    if (goal.todayMillis >= goal.goalMillis) {
                        stringResource(MR.strings.statistics_goal_met, formatDuration(goal.goalMillis))
                    } else {
                        stringResource(
                            MR.strings.statistics_goal_to_go,
                            formatDuration(goal.goalMillis - goal.todayMillis),
                            formatDuration(goal.goalMillis),
                        )
                    },
                )
            }
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.size(40.dp),
                // The tile's own surface tones are too close to each other in dark themes to show an empty ring.
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                strokeWidth = 5.dp,
                gapSize = 0.dp,
            )
        }
    }
}

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
    val period = formatCompactWindow(LocalContext.current, previousWindow)
    val color = when {
        delta > 0L -> MaterialTheme.colorScheme.primary
        delta < 0L -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (delta != 0L) {
            Icon(
                imageVector = if (delta > 0L) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color,
            )
        }
        TileDetail(stringResource(MR.strings.statistics_change_vs_period, change, period), color = color)
    }
}

/** Short localized range such as "Sep 15 – 21"; the year is kept only for yearly windows. */
private fun formatCompactWindow(context: Context, window: StatsActivityWindow): String {
    val zone = ZoneId.systemDefault()
    val start = (window.startDate ?: window.endDate).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = window.endDate.plusDays(1L).atStartOfDay(zone).toInstant().toEpochMilli()
    var flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    if (window.range != StatsRange.ONE_YEAR && window.endDate.year == LocalDate.now().year) {
        flags = flags or DateUtils.FORMAT_NO_YEAR
    }
    return DateUtils.formatDateRange(context, start, end, flags)
}

@Composable
private fun TileDetail(text: String, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
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
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            detail()
        }
    }
}
