package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.entry.components.presentation
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.more.stats.data.StatsLabelCount
import eu.kanade.presentation.more.stats.data.StatsLibraryInsights
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle

/** A single type's library: backlog, downloads, genres, publication status, sources and recent additions. */
@Composable
internal fun StatisticsLibraryInsightsCard(
    insights: StatsLibraryInsights,
    type: EntryType,
    titleCount: Int,
    color: Color,
) {
    val numbers = NumberFormat.getIntegerInstance()
    val typePresentation = type.entryTypePresentation()
    StatisticsSectionCard(title = stringResource(MR.strings.statistics_library_insights)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatisticsInsightTile(
                value = numbers.format(insights.unconsumedCount),
                label = stringResource(typePresentation.unconsumedIndicatorLabel),
                detail = pluralStringResource(
                    MR.plurals.statistics_backlog_title_count,
                    insights.titlesWithUnconsumed,
                    insights.titlesWithUnconsumed,
                ).takeIf { insights.titlesWithUnconsumed > 0 },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            StatisticsInsightTile(
                value = numbers.format(insights.downloadedCount),
                label = stringResource(MR.strings.statistics_downloaded),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
            StatisticsInsightTile(
                value = insights.categoryCount.toString(),
                label = stringResource(MR.strings.categories),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }
        if (insights.topGenres.isNotEmpty()) {
            InsightHeading(stringResource(MR.strings.statistics_genres))
            LabelCountBars(insights.topGenres, titleCount, color)
        }
        val statuses = insights.statusCounts.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
            .map { (status, count) -> StatsLabelCount(status.presentation().label, count) }
        if (statuses.isNotEmpty()) {
            InsightHeading(stringResource(MR.strings.statistics_status))
            LabelCountBars(statuses, titleCount, color)
        }
        if (insights.topSources.isNotEmpty()) {
            InsightHeading(stringResource(MR.strings.statistics_sources))
            LabelCountBars(insights.topSources, titleCount, color)
            if (insights.otherSourceCount > 0) {
                Text(
                    text = pluralStringResource(
                        MR.plurals.statistics_other_source_count,
                        insights.otherSourceCount,
                        insights.otherSourceCount,
                    ) + " · " + pluralStringResource(
                        MR.plurals.statistics_title_count,
                        insights.otherSourcesTitleCount,
                        insights.otherSourcesTitleCount,
                    ),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AddedByMonth(insights, color)
    }
}

@Composable
private fun AddedByMonth(insights: StatsLibraryInsights, color: Color) {
    val locale = LocalConfiguration.current.locales[0]
    val thisMonth = insights.addedByMonth.lastIndex
    val addedThisMonth = insights.addedByMonth.lastOrNull() ?: 0
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        InsightHeading(
            text = stringResource(MR.strings.statistics_added_in_year, insights.addedYear.toString()),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = pluralStringResource(MR.plurals.statistics_added_this_month, addedThisMonth, addedThisMonth),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    // Twelve slots keep bar widths stable through the year; months still ahead stay empty.
    val values = Month.entries.map { month -> insights.addedByMonth.getOrNull(month.ordinal)?.toLong() ?: 0L }
    StatisticsRhythmBars(
        values = values,
        labels = Month.entries.map { it.getDisplayName(TextStyle.NARROW, locale) },
        highlightIndex = thisMonth.takeIf { it >= 0 && insights.addedYear == LocalDate.now().year },
        color = color,
        description = values.take(thisMonth + 1).withIndex().joinToString { (index, count) ->
            "${Month.entries[index].getDisplayName(TextStyle.FULL, locale)} $count"
        },
        barAreaHeight = 48.dp,
    )
}

@Composable
private fun InsightHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 20.dp, bottom = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.titleSmall,
    )
}

/** Label, count and a bar scaled to the share of [total] titles. */
@Composable
private fun LabelCountBars(items: List<StatsLabelCount>, total: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(item.count.toString(), style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    progress = { if (total > 0) (item.count.toFloat() / total).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(4.dp),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }
        }
    }
}
