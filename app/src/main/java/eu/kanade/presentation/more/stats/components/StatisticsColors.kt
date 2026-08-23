package eu.kanade.presentation.more.stats.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import mihon.entry.interactions.statistics.EntryStatisticsAccent

@Composable
internal fun EntryStatisticsAccent.color(): Color {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return when (this) {
        EntryStatisticsAccent.ROSE -> if (dark) Color(0xFFE6A8BE) else Color(0xFFAD496F)
        EntryStatisticsAccent.SKY -> if (dark) Color(0xFF8DC5F4) else Color(0xFF2478B2)
        EntryStatisticsAccent.SAGE -> if (dark) Color(0xFFA9D29E) else Color(0xFF4D8248)
        EntryStatisticsAccent.PLUM -> if (dark) Color(0xFFCBB4F3) else Color(0xFF7352A6)
        EntryStatisticsAccent.AMBER -> if (dark) Color(0xFFE4B56F) else Color(0xFF95600C)
    }
}
