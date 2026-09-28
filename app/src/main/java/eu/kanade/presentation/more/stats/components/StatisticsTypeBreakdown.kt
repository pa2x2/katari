package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType

internal data class StatisticsTypeShare(
    val label: String,
    val duration: String,
    val color: Color,
)

/** Per-type shares with activity, for panels that describe more than one entry type. */
internal fun statisticsTypeShares(
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
                duration = formatDuration(it),
                color = typeColors.getValue(type.type),
            )
        }
    }
}

/**
 * With [reserveLine], keeps one line of height even when empty so the panel doesn't jump as the selection
 * changes; single-type pages never show shares and pass false.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
internal fun StatisticsTypeBreakdown(
    shares: List<StatisticsTypeShare>,
    reserveLine: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!reserveLine && shares.isEmpty()) return
    val lineHeight = with(LocalDensity.current) { MaterialTheme.typography.bodySmall.lineHeight.toDp() }
    FlowRow(
        modifier = modifier.padding(top = 6.dp).heightIn(min = lineHeight),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        shares.forEach { share ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(8.dp), color = share.color, shape = CircleShape, content = {})
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${share.label} · ${share.duration}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                )
            }
        }
    }
}
