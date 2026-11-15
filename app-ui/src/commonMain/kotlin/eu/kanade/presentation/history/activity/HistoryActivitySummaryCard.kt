package eu.kanade.presentation.history.activity

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import app.katari.presentation.kit.content.Figure
import app.katari.presentation.kit.content.LeadFigure
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.surface.DashboardCard
import app.katari.presentation.kit.theme.KatariTheme
import eu.kanade.domain.ui.bestDateTimePattern
import eu.kanade.presentation.more.stats.components.StatisticsRhythmBars
import eu.kanade.presentation.more.stats.components.StatisticsTypeBars
import eu.kanade.presentation.more.stats.components.StatisticsTypeSplit
import eu.kanade.presentation.more.stats.components.peakHour
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.components.statisticsTypeShares
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.presentation.util.rememberHourSkeleton
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivitySummary
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Whole-range totals above an activity list: the time with its sessions and finished count, the time by hour (one day)
 * or by day or month in the types' colours, and the split by type when there is more than one.
 */
@Composable
internal fun HistoryActivitySummaryCard(
    summary: HistoryActivitySummary,
    types: List<StatsType>,
    colors: Map<EntryType, Color>,
) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val numbers = NumberFormat.getIntegerInstance()
    DashboardCard {
        LeadFigure(
            value = formatDuration(summary.totalDurationMillis),
            facts = listOf(
                Figure(
                    value = numbers.format(summary.sessionCount),
                    label = pluralStringResource(
                        MR.plurals.statistics_sessions_unit,
                        summary.sessionCount.toInt(),
                    ),
                ),
                Figure(
                    value = numbers.format(summary.completionCount),
                    label = pluralStringResource(
                        MR.plurals.statistics_finished_unit,
                        summary.completionCount.toInt(),
                    ),
                ),
            ),
        )
        summary.hourlyDurationMillis?.let { hourly ->
            HourlyBars(
                hourly = hourly,
                color = summary.durationByType.keys.singleOrNull()?.let(colors::get) ?: KatariTheme.chart.series,
            )
        }
        if (summary.buckets.isNotEmpty()) {
            PeriodBars(summary, colors, formatDuration)
        }
        StatisticsTypeSplit(
            shares = statisticsTypeShares(
                durationByType = summary.durationByType,
                types = types,
                typeLabels = types.associate { it.type to stringResource(it.displayName) },
                typeColors = colors,
                formatDuration = formatDuration,
            ),
            modifier = Modifier.padding(top = KatariTheme.spacing.m),
        )
    }
}

@Composable
private fun HourlyBars(hourly: List<Long>, color: Color) {
    val locale = LocalLocale.current.platformLocale
    val hourSkeleton = rememberHourSkeleton()
    val hourFormatter = remember(locale, hourSkeleton) {
        DateTimeFormatter.ofPattern(bestDateTimePattern(locale, hourSkeleton), locale)
    }
    val formatHour = { hour: Int -> LocalTime.of(hour, 0).format(hourFormatter) }
    val peak = peakHour(hourly)
    StatisticsRhythmBars(
        values = hourly,
        labels = hourly.indices.map { hour -> formatHour(hour).takeIf { hour % 6 == 0 } },
        highlightIndex = peak,
        color = color,
        description = peak?.let { stringResource(MR.strings.statistics_peak_hour, formatHour(it)) }.orEmpty(),
        barAreaHeight = StatisticsMetrics.rhythmHeight,
        modifier = Modifier.padding(top = KatariTheme.spacing.l),
    )
}

@Composable
private fun PeriodBars(
    summary: HistoryActivitySummary,
    colors: Map<EntryType, Color>,
    formatDuration: (Long) -> String,
) {
    val locale = LocalLocale.current.platformLocale
    val buckets = summary.buckets
    val sameYear = buckets.first().startDate.year == buckets.last().startDate.year
    val skeleton = when {
        !summary.bucketsAreMonths -> "MMMd"
        sameYear -> "MMM"
        else -> "MMMyy"
    }
    val formatter = remember(skeleton, locale) {
        DateTimeFormatter.ofPattern(bestDateTimePattern(locale, skeleton), locale)
    }
    val lastIndex = buckets.lastIndex
    // Counted back from the latest bar, so the range's last day or month is always named.
    val step = (buckets.size + MAX_LABELS - 1) / MAX_LABELS
    val busiest = buckets.indices.filter { buckets[it].durationMillis > 0L }.maxByOrNull { buckets[it].durationMillis }
    StatisticsTypeBars(
        bars = buckets.map { it.durationByType },
        colors = colors,
        labels = buckets.mapIndexed { index, bucket ->
            bucket.startDate.format(formatter).takeIf { (lastIndex - index) % step == 0 }
        },
        description = busiest?.let {
            stringResource(
                MR.strings.statistics_busiest_point,
                buckets[it].startDate.format(formatter),
                formatDuration(buckets[it].durationMillis),
            )
        }.orEmpty(),
        barAreaHeight = StatisticsMetrics.rhythmCompactHeight,
        modifier = Modifier.padding(top = KatariTheme.spacing.l),
    )
}

/** Enough to place a range's bars in time without the labels running into each other. */
private const val MAX_LABELS = 6
