package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.presentation.kit.content.LegendSwatch
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole

/** Names the chart's series when it stacks more than one entry type. */
@Composable
internal fun StatisticsTrendLegend(
    types: List<StatsType>,
    typeLabels: Map<EntryType, String>,
    typeColors: Map<EntryType, Color>,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l)) {
        types.forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                LegendSwatch(typeColors.getValue(type.type))
                Text(text = typeLabels.getValue(type.type), role = TextRole.Supporting)
            }
        }
    }
}
