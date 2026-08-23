package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsLibraryInsights
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsAdditionalInsightsCard(
    typeLabel: String,
    sessionCount: Long,
    averageSessionDurationMillis: Long,
    longestSessionDurationMillis: Long,
    activeDays: Int,
    library: StatsLibraryInsights,
    formatDuration: (Long) -> String,
) {
    StatisticsSectionCard(
        title = stringResource(MR.strings.statistics_more_type_stats, typeLabel.lowercase()),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2,
        ) {
            if (sessionCount > 0L) {
                InsightItem(
                    value = formatDuration(averageSessionDurationMillis),
                    label = stringResource(MR.strings.statistics_average_visit),
                    modifier = Modifier.weight(1f).widthIn(min = 150.dp),
                )
                InsightItem(
                    value = formatDuration(longestSessionDurationMillis),
                    label = stringResource(MR.strings.statistics_longest_visit),
                    modifier = Modifier.weight(1f).widthIn(min = 150.dp),
                )
            }
            InsightItem(
                value = activeDays.toString(),
                label = stringResource(MR.strings.statistics_active_days),
                modifier = Modifier.weight(1f).widthIn(min = 150.dp),
            )
            library.topGenre?.let { genre ->
                InsightItem(
                    value = genre,
                    label = stringResource(MR.strings.statistics_top_genre),
                    modifier = Modifier.weight(1f).widthIn(min = 150.dp),
                )
            }
            InsightItem(
                value = library.categoryCount.toString(),
                label = stringResource(MR.strings.categories),
                modifier = Modifier.weight(1f).widthIn(min = 150.dp),
            )
            InsightItem(
                value = library.sourceCount.toString(),
                label = stringResource(MR.strings.label_sources),
                modifier = Modifier.weight(1f).widthIn(min = 150.dp),
            )
        }
    }
}

@Composable
private fun InsightItem(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
