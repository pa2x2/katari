package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsProgress
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Overview-only: each type's title count and progress, opening that type's tab. */
@Composable
internal fun StatisticsMediaCard(
    titleCounts: Map<EntryType, Int>,
    progressByType: Map<EntryType, StatsProgress>,
    types: List<StatsType>,
    onTypeClick: (EntryType) -> Unit,
) {
    StatisticsSectionCard(title = stringResource(MR.strings.statistics_by_media)) {
        types.forEachIndexed { index, type ->
            val count = titleCounts[type.type] ?: 0
            val label = stringResource(type.displayName)
            val color = type.accent.color()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTypeClick(type.type) }
                    .semantics { contentDescription = "$label, $count" }
                    .padding(vertical = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(10.dp), color = color, shape = CircleShape, content = {})
                    Spacer(Modifier.width(10.dp))
                    Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(count.toString(), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                progressByType[type.type]?.let { progress ->
                    StatisticsProgressBar(
                        progress = progress,
                        color = color,
                        height = 6.dp,
                        modifier = Modifier
                            .padding(start = 20.dp, top = 8.dp, end = 26.dp)
                            .clearAndSetSemantics {},
                    )
                }
            }
            if (index != types.lastIndex) HorizontalDivider()
        }
    }
}
