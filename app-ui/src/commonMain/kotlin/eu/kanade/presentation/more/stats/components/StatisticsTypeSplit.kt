package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import app.katari.presentation.kit.content.LegendSwatch
import app.katari.presentation.kit.content.StatisticsMetrics
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.content.chartBar
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType

data class StatisticsTypeShare(
    val label: String,
    val durationMillis: Long,
    val duration: String,
    val color: Color,
)

/** Per-type shares with activity, for panels that describe more than one entry type. */
fun statisticsTypeShares(
    durationByType: Map<EntryType, Long>,
    types: List<StatsType>,
    typeLabels: Map<EntryType, String>,
    typeColors: Map<EntryType, Color>,
    formatDuration: (Long) -> String,
): List<StatisticsTypeShare> {
    if (types.size <= 1) return emptyList()
    return types.mapNotNull { type ->
        durationByType[type.type]?.takeIf { it > 0L }?.let {
            StatisticsTypeShare(
                label = typeLabels.getValue(type.type),
                durationMillis = it,
                duration = formatDuration(it),
                color = typeColors.getValue(type.type),
            )
        }
    }
}

/**
 * Time by type as one bar split in the types' colours, largest first, with each type's name and time under it in the
 * same order, so the bar needs no legend of its own. Nothing is drawn for fewer than two types.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
fun StatisticsTypeSplit(
    shares: List<StatisticsTypeShare>,
    modifier: Modifier = Modifier,
) {
    if (shares.size < 2) return
    val ordered = shares.sortedByDescending(StatisticsTypeShare::durationMillis)
    val total = ordered.sumOf(StatisticsTypeShare::durationMillis).toFloat()
    val spacing = KatariTheme.spacing
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.s)) {
        Canvas(Modifier.fillMaxWidth().height(StatisticsMetrics.shareBarThick).chartBar()) {
            val gap = spacing.xxs.toPx()
            val room = size.width - gap * (ordered.size - 1)
            var left = 0f
            ordered.forEach { share ->
                val width = room * share.durationMillis / total
                drawRect(share.color, Offset(left, 0f), Size(width, size.height))
                left += width + gap
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.l),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            ordered.forEach { share ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendSwatch(share.color)
                    Text(text = share.label, role = TextRole.Supporting, maxLines = 1)
                    Text(text = share.duration, role = TextRole.Label, maxLines = 1)
                }
            }
        }
    }
}
