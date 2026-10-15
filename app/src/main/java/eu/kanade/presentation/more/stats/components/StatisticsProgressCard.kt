package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.presentation.more.stats.data.StatsProgress
import mihon.presentation.kit.content.LegendSwatch
import mihon.presentation.kit.content.StatisticsMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

@Composable
internal fun StatisticsProgressCard(progress: StatsProgress?, color: Color) {
    StatisticsSectionCard(
        title = stringResource(MR.strings.statistics_progress),
        contentSpacing = KatariTheme.spacing.m,
    ) {
        if (progress == null) {
            Text(text = stringResource(MR.strings.not_applicable), tone = ContentTone.Secondary)
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
            height = StatisticsMetrics.stageBar,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Column(
            Modifier.padding(top = KatariTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            labels.indices.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l)) {
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
                modifier = Modifier.padding(top = KatariTheme.spacing.m),
                role = TextRole.Supporting,
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
        LegendSwatch(color)
        Text(
            text = label,
            modifier = Modifier.weight(1f).padding(start = KatariTheme.spacing.s),
            role = TextRole.Supporting,
            maxLines = 1,
        )
        Text(count.toString(), role = TextRole.Label)
        Text(
            text = share,
            modifier = Modifier.width(StatisticsMetrics.percentWidth),
            role = TextRole.Badge,
            tone = ContentTone.Secondary,
            textAlign = TextAlign.End,
        )
    }
}
