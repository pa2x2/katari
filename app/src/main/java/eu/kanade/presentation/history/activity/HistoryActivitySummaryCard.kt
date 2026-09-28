package eu.kanade.presentation.history.activity

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.StatisticsInsightTile
import eu.kanade.presentation.more.stats.components.StatisticsRhythmBars
import eu.kanade.presentation.more.stats.components.StatisticsSectionCard
import eu.kanade.presentation.more.stats.components.StatisticsTypeBreakdown
import eu.kanade.presentation.more.stats.components.color
import eu.kanade.presentation.more.stats.components.peakHour
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.components.statisticsTypeShares
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivitySummary
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Whole-range totals above the session list, with the time spread by hour (one day) or by day/month. */
@Composable
internal fun HistoryActivitySummaryCard(
    summary: HistoryActivitySummary,
    type: EntryType?,
    types: List<StatsType>,
) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val numbers = NumberFormat.getIntegerInstance()
    val locale = LocalConfiguration.current.locales[0]
    val selectedType = types.firstOrNull { it.type == type }
    val color = selectedType?.accent?.color() ?: MaterialTheme.colorScheme.primary
    StatisticsSectionCard(contentSpacing = 0.dp) {
        Text(
            text = formatDuration(summary.totalDurationMillis),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        if (type == null) {
            StatisticsTypeBreakdown(
                statisticsTypeShares(
                    durationByType = summary.durationByType,
                    types = types,
                    typeLabels = types.associate { it.type to stringResource(it.displayName) },
                    typeColors = types.associate { it.type to it.accent.color() },
                    formatDuration = formatDuration,
                ),
                reserveLine = false,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatisticsInsightTile(
                value = numbers.format(summary.sessionCount),
                label = stringResource(MR.strings.statistics_sessions),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            StatisticsInsightTile(
                value = numbers.format(summary.completionCount),
                label = selectedType?.let { stringResource(it.consumedUnitLabel) }
                    ?: stringResource(MR.strings.statistics_completed),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        summary.hourlyDurationMillis?.let { hourly ->
            val hourFormatter = remember(locale) {
                DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "j"), locale)
            }
            val formatHour = { hour: Int -> LocalTime.of(hour, 0).format(hourFormatter) }
            val peak = peakHour(hourly)
            StatisticsRhythmBars(
                values = hourly,
                labels = hourly.indices.map { hour -> formatHour(hour).takeIf { hour % 6 == 0 } },
                highlightIndex = peak,
                color = color,
                description = peak?.let { stringResource(MR.strings.statistics_peak_hour, formatHour(it)) }.orEmpty(),
                barAreaHeight = 56.dp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        if (summary.buckets.isNotEmpty()) {
            val pattern = if (summary.bucketsAreMonths) "MMM yy" else "MMM d"
            val formatter = remember(pattern, locale) { DateTimeFormatter.ofPattern(pattern, locale) }
            val values = summary.buckets.map { it.durationMillis }
            val lastIndex = values.lastIndex
            val busiest = values.indices.filter { values[it] > 0L }.maxByOrNull { values[it] }
            StatisticsRhythmBars(
                values = values,
                // Endpoints and the middle anchor the axis without crowding short labels together.
                labels = summary.buckets.mapIndexed { index, bucket ->
                    bucket.startDate.format(formatter).takeIf {
                        index == 0 || index == lastIndex ||
                            index == lastIndex / 2
                    }
                },
                highlightIndex = busiest,
                color = color,
                description = busiest?.let {
                    stringResource(
                        MR.strings.statistics_busiest_point,
                        summary.buckets[it].startDate.format(formatter),
                        formatDuration(values[it]),
                    )
                }.orEmpty(),
                barAreaHeight = 56.dp,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}
