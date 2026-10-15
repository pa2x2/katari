package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ChevronRight
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
        modifier = Modifier.fillMaxWidth().padding(top = KatariTheme.spacing.m),
        enabled = hasActivity,
        container = SurfaceContainer.High,
        shape = KatariTheme.shapes.medium,
    ) {
        Column(Modifier.padding(horizontal = KatariTheme.spacing.l, vertical = KatariTheme.spacing.m)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDuration(summary.totalDurationMillis),
                    modifier = Modifier.weight(1f),
                    role = TextRole.SectionTitle,
                )
                if (hasActivity) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                        tone = ContentTone.Secondary,
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
                modifier = Modifier.padding(top = KatariTheme.spacing.xxs),
                role = TextRole.Supporting,
            )
            StatisticsTypeBreakdown(
                statisticsTypeShares(summary.durationByType, types, typeLabels, typeColors, formatDuration),
                reserveLine = types.size > 1,
            )
        }
    }
}
