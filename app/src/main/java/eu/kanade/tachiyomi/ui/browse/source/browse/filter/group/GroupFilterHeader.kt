package eu.kanade.tachiyomi.ui.browse.source.browse.filter.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterGroupSummary
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.activeCount
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.canClear
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChangeBadges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.clearSelection
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.metadata
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.MoreVert
import mihon.icons.materialsymbols.rounded.RestartAlt
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.content.Icon
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.content.TonedSpan
import mihon.presentation.kit.content.TonedText
import mihon.presentation.kit.list.RowGroupSeparator
import mihon.presentation.kit.list.currentRowInset
import mihon.presentation.kit.menu.DropdownMenu
import mihon.presentation.kit.menu.DropdownMenuItem
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
    val hostSummary = filter.selectionSummary(changes).takeUnless { it.isEmpty }?.spans()
    val subtitle = sourceSummary?.let { listOf(TonedSpan(it)) }
        ?: hostSummary
        ?: filter.metadata?.description?.takeIf { it.isNotBlank() }?.let { listOf(TonedSpan(it)) }
    val clearDiffersFromReset = filter.canClear() && !changes.hasEmptyDefault(filter) && filter.activeCount() != 0
    val spacing = KatariTheme.spacing
    Column {
        if (leadingDivider) RowGroupSeparator()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { state.expanded = !state.expanded }
                .padding(start = currentRowInset, end = spacing.s, top = spacing.m, bottom = spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = filter.name,
                    role = TextRole.Label,
                    tone = if (hasIssues) ContentTone.Error else ContentTone.Inherit,
                )
                subtitle?.let {
                    TonedText(
                        spans = it,
                        role = TextRole.Supporting,
                        separator = " · ",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            FilterChangeBadges(change, hasIssues = hasIssues)
            if (change.isChanged) {
                IconButton(
                    icon = MaterialSymbols.Rounded.RestartAlt,
                    contentDescription = stringResource(MR.strings.filter_reset_group, filter.name),
                    onClick = {
                        onReset(filter)
                        onUpdate()
                    },
                    tone = ContentTone.Primary,
                )
            }
            if (clearDiffersFromReset) {
                GroupClearMenu(filter.name) {
                    filter.clearSelection()
                    onUpdate()
                }
            }
            Icon(
                imageVector = if (state.expanded) {
                    MaterialSymbols.Rounded.KeyboardArrowUp
                } else {
                    MaterialSymbols.Rounded.KeyboardArrowDown
                },
                contentDescription = null,
                modifier = Modifier.padding(horizontal = spacing.xs),
            )
        }
    }
}

@Composable
private fun GroupClearMenu(groupName: String, onClear: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(
            icon = MaterialSymbols.Rounded.MoreVert,
            contentDescription = stringResource(MR.strings.filter_group_actions, groupName),
            onClick = { expanded = true },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                label = stringResource(MR.strings.filter_clear),
                onClick = {
                    expanded = false
                    onClear()
                },
            )
        }
    }
}
