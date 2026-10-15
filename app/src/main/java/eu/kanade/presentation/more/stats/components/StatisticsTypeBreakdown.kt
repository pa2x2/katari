package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.presentation.kit.content.LegendSwatch
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.lineHeight
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

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
    FlowRow(
        modifier = modifier.padding(top = KatariTheme.spacing.s).heightIn(min = TextRole.Supporting.lineHeight),
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs),
    ) {
        shares.forEach { share ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                LegendSwatch(share.color)
                Text(text = "${share.label} · ${share.duration}", role = TextRole.Supporting, maxLines = 1)
            }
        }
    }
}
