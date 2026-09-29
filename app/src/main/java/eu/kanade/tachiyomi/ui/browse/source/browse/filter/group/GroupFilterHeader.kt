package eu.kanade.tachiyomi.ui.browse.source.browse.filter.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterGroupSummary
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChangeBadges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSheetInsets

/** The tappable row that names a group, summarizes its selection, and opens or closes it. */
@Composable
internal fun GroupFilterHeader(
    filter: EntryFilter.Group<*>,
    state: FilterGroupUiState,
    changes: FilterChanges,
    changedOnly: Boolean,
    leadingDivider: Boolean,
) {
    val hasIssues = filter.validationIssues().isNotEmpty()
    LaunchedEffect(hasIssues) { if (hasIssues) state.expanded = true }
    LaunchedEffect(changedOnly) { if (changedOnly) state.expanded = true }
    val summary = (filter as? EntryFilterGroupSummary)?.selectionSummary(
        filter.state.filterIsInstance<EntryFilter<*>>(),
    )
    Column {
        if (leadingDivider) HorizontalDivider(Modifier.padding(horizontal = FilterSheetInsets.Horizontal))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { state.expanded = !state.expanded }
                .padding(FilterSheetInsets.Horizontal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    filter.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (hasIssues) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                summary?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FilterChangeBadges(changes[filter], hasIssues = hasIssues)
            Icon(
                if (state.expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
            )
        }
    }
}
