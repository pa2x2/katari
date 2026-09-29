package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.temporal.ChronoUnit

internal data class TrendPeriodSummary(
    val totalDurationMillis: Long,
    val durationByType: Map<EntryType, Long>,
    /** Days on or after tracking started; the daily average divides by these. */
    val trackedDays: Long,
    val busiest: StatsTrendPoint?,
) {
    val dailyAverageMillis: Long = if (trackedDays > 0L) totalDurationMillis / trackedDays else 0L
}

internal fun summarizeTrendPeriod(points: List<StatsTrendPoint>): TrendPeriodSummary {
    val tracked = points.filter(StatsTrendPoint::isTracked)
    return TrendPeriodSummary(
        totalDurationMillis = tracked.sumOf(StatsTrendPoint::totalDurationMillis),
        durationByType = tracked.flatMap { it.durationByType.entries }
            .groupBy({ it.key }, { it.value })
            .mapValues { (_, durations) -> durations.sum() },
        trackedDays = tracked.sumOf { point ->
            ChronoUnit.DAYS.between(checkNotNull(point.trackedStartDate), point.endDate) + 1L
        },
        busiest = tracked.filter { it.totalDurationMillis > 0L }.maxByOrNull(StatsTrendPoint::totalDurationMillis),
    )
}

/** Panel shown under the chart while no bar is selected; opens the whole period's activity. */
@Composable
internal fun StatisticsTrendPeriodSummary(
    summary: TrendPeriodSummary,
    types: List<StatsType>,
    typeLabels: Map<EntryType, String>,
    typeColors: Map<EntryType, Color>,
    formatDuration: (Long) -> String,
    formatBusiest: (StatsTrendPoint) -> String,
    onOpen: () -> Unit,
) {
    val hasActivity = summary.totalDurationMillis > 0L
    Surface(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        enabled = hasActivity,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDuration(summary.totalDurationMillis),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (hasActivity) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = if (hasActivity) {
                    buildString {
                        append(
                            stringResource(MR.strings.statistics_per_day, formatDuration(summary.dailyAverageMillis)),
                        )
                        summary.busiest?.let { busiest ->
                            append(" · ")
                            append(
                                stringResource(
                                    MR.strings.statistics_busiest_point,
                                    formatBusiest(busiest),
                                    formatDuration(busiest.totalDurationMillis),
                                ),
                            )
                        }
                    }
                } else {
                    stringResource(MR.strings.statistics_empty_period)
                },
                modifier = Modifier.padding(top = 2.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatisticsTypeBreakdown(
                statisticsTypeShares(summary.durationByType, types, typeLabels, typeColors, formatDuration),
                reserveLine = types.size > 1,
            )
        }
    }
}
