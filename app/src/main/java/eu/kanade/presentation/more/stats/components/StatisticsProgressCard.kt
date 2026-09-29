package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsProgress
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

@Composable
internal fun StatisticsProgressCard(progress: StatsProgress?, color: Color) {
    StatisticsSectionCard(title = stringResource(MR.strings.statistics_progress), contentSpacing = 12.dp) {
        if (progress == null) {
            Text(
                text = stringResource(MR.strings.not_applicable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@StatisticsSectionCard
        }
        val labels = listOf(
            stringResource(MR.strings.statistics_not_started),
            stringResource(MR.strings.statistics_in_progress),
            stringResource(MR.strings.statistics_caught_up),
            stringResource(MR.strings.completed),
        )
        val counts = progress.stageCounts()
        val colors = statisticsProgressStageColors(color)
        val total = progress.total
        StatisticsProgressBar(
            progress = progress,
            color = color,
            height = 12.dp,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.indices.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEach { index ->
                        ProgressLegendItem(
                            label = labels[index],
                            count = counts[index],
                            percent = if (total > 0) (counts[index] * 100f / total).roundToInt() else 0,
                            color = colors[index],
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (progress.isPartial) {
            Text(
                text = stringResource(
                    MR.strings.statistics_progress_coverage,
                    progress.total,
                    progress.libraryTotal,
                ),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressLegendItem(label: String, count: Int, percent: Int, color: Color, modifier: Modifier) {
    val share = stringResource(MR.strings.statistics_share_of_time, percent)
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$label, $count, $share" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(Modifier.size(10.dp), color = color, shape = CircleShape, content = {})
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(count.toString(), style = MaterialTheme.typography.labelLarge)
        Text(
            text = share,
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}
