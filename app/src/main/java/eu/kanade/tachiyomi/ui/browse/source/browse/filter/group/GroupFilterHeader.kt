package eu.kanade.tachiyomi.ui.browse.source.browse.filter.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterGroupSummary
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.activeCount
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.canClear
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChangeBadges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.clearSelection
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSheetInsets
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.metadata
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The tappable row that names a group, summarizes its selection, and opens or closes it.
 *
 * The subtitle is the source's own summary, else the host summary of the current selection, else the group's
 * description. A reset action appears only while the group differs from its defaults, and clearing is offered
 * separately only when it would do something other than resetting.
 */
@Composable
internal fun GroupFilterHeader(
    filter: EntryFilter.Group<*>,
    state: FilterGroupUiState,
    changes: FilterChanges,
    changedOnly: Boolean,
    leadingDivider: Boolean,
    onReset: (EntryFilter<*>) -> Unit,
    onUpdate: () -> Unit,
) {
    val hasIssues = filter.validationIssues().isNotEmpty()
    LaunchedEffect(hasIssues) { if (hasIssues) state.expanded = true }
    LaunchedEffect(changedOnly) { if (changedOnly) state.expanded = true }
    val change = changes[filter]
    val sourceSummary = (filter as? EntryFilterGroupSummary)
        ?.selectionSummary(filter.state.filterIsInstance<EntryFilter<*>>())
        ?.takeIf { it.isNotBlank() }
    val hostSummary = filter.selectionSummary(changes).takeUnless { it.isEmpty }?.text()
    val subtitle = sourceSummary?.let(::AnnotatedString)
        ?: hostSummary
        ?: filter.metadata?.description?.takeIf { it.isNotBlank() }?.let(::AnnotatedString)
    val clearDiffersFromReset = filter.canClear() && !changes.hasEmptyDefault(filter) && filter.activeCount() != 0
    Column {
        if (leadingDivider) HorizontalDivider(Modifier.padding(horizontal = FilterSheetInsets.Horizontal))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { state.expanded = !state.expanded }
                .padding(start = FilterSheetInsets.Horizontal, end = 8.dp, top = 8.dp, bottom = 8.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    filter.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (hasIssues) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FilterChangeBadges(change, hasIssues = hasIssues)
            if (change.isChanged) {
                IconButton(onClick = {
                    onReset(filter)
                    onUpdate()
                }) {
                    Icon(
                        Icons.Outlined.RestartAlt,
                        contentDescription = stringResource(MR.strings.filter_reset_group, filter.name),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (clearDiffersFromReset) {
                GroupClearMenu(filter.name) {
                    filter.clearSelection()
                    onUpdate()
                }
            }
            Icon(
                if (state.expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

@Composable
private fun GroupClearMenu(groupName: String, onClear: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Outlined.MoreVert,
                contentDescription = stringResource(MR.strings.filter_group_actions, groupName),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(MR.strings.filter_clear)) },
                onClick = {
                    expanded = false
                    onClear()
                },
            )
        }
    }
}
