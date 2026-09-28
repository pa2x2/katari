package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun StatisticsTrendSelection(
    selected: StatsTrendPoint,
    types: List<StatsType>,
    typeLabels: Map<EntryType, String>,
    typeColors: Map<EntryType, Color>,
    formattedDate: String,
    formattedDuration: String,
    notTrackedLabel: String,
    formatDuration: (Long) -> String,
    actionLabel: String?,
    onOpenActivity: (StatsTrendPoint) -> Unit,
    onClearSelection: () -> Unit,
) {
    val isActionable = isTrendSelectionActionable(selected, hasAlternateAction = actionLabel != null)
    val shares = if (selected.isTracked) {
        statisticsTypeShares(selected.durationByType, types, typeLabels, typeColors, formatDuration)
    } else {
        emptyList()
    }

    Surface(
        onClick = { onOpenActivity(selected) },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        enabled = isActionable,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedDate,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (selected.isTracked) formattedDuration else notTrackedLabel,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (isActionable) {
                    Spacer(Modifier.width(4.dp))
                    actionLabel?.let { label ->
                        Text(
                            text = label,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                        Spacer(Modifier.width(2.dp))
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClearSelection, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(MR.strings.statistics_show_whole_period),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            StatisticsTypeBreakdown(shares)
        }
    }
}

internal fun isTrendSelectionActionable(
    selected: StatsTrendPoint,
    hasAlternateAction: Boolean,
): Boolean = selected.isTracked && (selected.totalDurationMillis > 0L || hasAlternateAction)
