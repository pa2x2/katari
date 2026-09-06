package eu.kanade.presentation.more.stats.data

import androidx.compose.ui.graphics.vector.ImageVector
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.statistics.EntryStatisticsAccent

data class StatsType(
    val type: EntryType,
    val displayName: StringResource,
    val icon: ImageVector,
    val accent: EntryStatisticsAccent,
    val consumedUnitLabel: StringResource,
)
