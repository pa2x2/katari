package eu.kanade.presentation.more.stats.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.layout.DashboardTiles
import app.katari.presentation.kit.surface.DashboardTile
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat

/**
 * The period's figures under its chart: the daily average, the streak, what was finished and the sessions, each a
 * figure, what it counts and one detail. [activity] is already projected to [selectedType]; [types] are the
 * Overview's types, which the finished tile's detail names.
 */
@Composable
internal fun StatisticsPeriodTiles(
    activity: StatsActivity?,
    selectedType: StatsType?,
    types: List<StatsType>,
    formatDuration: (Long) -> String,
    modifier: Modifier = Modifier,
) {
    val numbers = NumberFormat.getIntegerInstance()
    val dash = "—"
    val isLatest = activity?.window?.isLatest != false
    val tracked = activity?.takeIf { it.trackedDayCount > 0 }
    val hasSessions = activity != null && activity.sessionCount > 0L
    val dailyAverage: @Composable (Modifier) -> Unit = { tileModifier ->
        PeriodTile(
            figure = tracked?.let { formatDuration(it.totalDurationMillis / it.trackedDayCount) } ?: dash,
            label = stringResource(MR.strings.statistics_a_day_unit),
            detail = tracked?.let {
                pluralStringResource(
                    MR.plurals.statistics_days_active_of,
                    it.trackedDayCount,
                    it.activeDays,
                    it.trackedDayCount,
                )
            },
            modifier = tileModifier,
        )
    }
    val streak: @Composable (Modifier) -> Unit = { tileModifier ->
        PeriodTile(
            figure = activity?.let { pluralStringResource(MR.plurals.day, it.currentStreakDays, it.currentStreakDays) }
                ?: dash,
            label = stringResource(
                if (isLatest) MR.strings.statistics_in_a_row else MR.strings.statistics_in_a_row_at_end,
            ),
            detail = activity?.let {
                stringResource(
                    MR.strings.statistics_best_streak,
                    pluralStringResource(MR.plurals.day, it.longestStreakDays, it.longestStreakDays),
                )
            },
            modifier = tileModifier,
        )
    }
    val finished: @Composable (Modifier) -> Unit = { tileModifier ->
        val count = activity?.completionCount ?: 0L
        PeriodTile(
            figure = activity?.completionCount?.let(numbers::format) ?: dash,
            label = pluralStringResource(MR.plurals.statistics_finished_unit, count.toInt()),
            detail = activity?.let {
                statisticsCompletionDetail(it, selectedType?.let(::listOf) ?: types, formatDuration)
            },
            modifier = tileModifier,
        )
    }
    val sessions: @Composable (Modifier) -> Unit = { tileModifier ->
        val count = activity?.sessionCount ?: 0L
        PeriodTile(
            figure = activity?.sessionCount?.let(numbers::format) ?: dash,
            label = pluralStringResource(MR.plurals.statistics_sessions_unit, count.toInt()),
            detail = activity?.takeIf { hasSessions }?.let {
                stringResource(MR.strings.statistics_on_average, formatDuration(it.averageSessionDurationMillis))
            },
            modifier = tileModifier,
        )
    }
    DashboardTiles(tiles = listOf(dailyAverage, streak, finished, sessions), modifier = modifier)
}

@Composable
private fun PeriodTile(figure: String, label: String, detail: String?, modifier: Modifier) {
    DashboardTile(modifier = modifier) {
        Text(text = figure, role = TextRole.Metric, maxLines = 1)
        Text(text = label, role = TextRole.Body, maxLines = 1)
        detail?.let {
            Text(text = it, role = TextRole.Supporting, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
