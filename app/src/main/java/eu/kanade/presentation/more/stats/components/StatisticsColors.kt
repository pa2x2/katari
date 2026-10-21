package eu.kanade.presentation.more.stats.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import mihon.entry.interactions.statistics.EntryStatisticsAccent
import mihon.presentation.kit.theme.KatariTheme

@Composable
@ReadOnlyComposable
internal fun EntryStatisticsAccent.color(): Color {
    val accents = KatariTheme.chart.accents
    return when (this) {
        EntryStatisticsAccent.ROSE -> accents[0]
        EntryStatisticsAccent.SKY -> accents[1]
        EntryStatisticsAccent.SAGE -> accents[2]
        EntryStatisticsAccent.PLUM -> accents[3]
        EntryStatisticsAccent.AMBER -> accents[4]
    }
}
