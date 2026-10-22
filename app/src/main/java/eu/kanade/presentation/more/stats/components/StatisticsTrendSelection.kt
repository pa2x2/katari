package eu.kanade.presentation.more.stats.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.presentation.more.stats.data.StatsType
import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.ChevronRight
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.DashboardCardFooter
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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

    DashboardCardFooter(onClick = { onOpenActivity(selected) }, enabled = isActionable) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formattedDate,
                modifier = Modifier.weight(1f),
                role = TextRole.Label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(KatariTheme.spacing.m))
            Text(text = if (selected.isTracked) formattedDuration else notTrackedLabel, role = TextRole.Label)
            if (isActionable) {
                Spacer(Modifier.width(KatariTheme.spacing.xs))
                actionLabel?.let { label ->
                    Text(text = label, role = TextRole.Label, tone = ContentTone.Primary, maxLines = 1)
                    Spacer(Modifier.width(KatariTheme.spacing.xxs))
                }
                Icon(
                    imageVector = MaterialSymbols.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(KatariTheme.sizes.iconSmall),
                    tone = ContentTone.Secondary,
                )
            }
            IconButton(
                icon = MaterialSymbols.Rounded.Close,
                contentDescription = stringResource(MR.strings.statistics_show_whole_period),
                onClick = onClearSelection,
                tone = ContentTone.Secondary,
            )
        }
        StatisticsTypeBreakdown(shares, reserveLine = types.size > 1)
    }
}

internal fun isTrendSelectionActionable(
    selected: StatsTrendPoint,
    hasAlternateAction: Boolean,
): Boolean = selected.isTracked && (selected.totalDurationMillis > 0L || hasAlternateAction)
