package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType

@Composable
@OptIn(ExperimentalLayoutApi::class)
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
) {
    val isActionable = isTrendSelectionActionable(selected, hasAlternateAction = actionLabel != null)
    val breakdownRowHeight = with(LocalDensity.current) {
        MaterialTheme.typography.bodySmall.lineHeight.toDp()
    }
    val breakdown = if (selected.isTracked && types.size > 1) {
        types.mapNotNull { type ->
            val duration = selected.durationByType[type.type] ?: 0L
            duration.takeIf { it > 0L }?.let {
                SelectionBreakdown(
                    label = typeLabels.getValue(type.type),
                    duration = formatDuration(it),
                    color = typeColors.getValue(type.type),
                )
            }
        }
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
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
            }
            FlowRow(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .heightIn(min = breakdownRowHeight),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                breakdown.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            color = item.color,
                            shape = CircleShape,
                            content = {},
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${item.label} · ${item.duration}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

internal fun isTrendSelectionActionable(
    selected: StatsTrendPoint,
    hasAlternateAction: Boolean,
): Boolean = selected.isTracked && (selected.totalDurationMillis > 0L || hasAlternateAction)

private data class SelectionBreakdown(
    val label: String,
    val duration: String,
    val color: Color,
)
