package eu.kanade.tachiyomi.ui.browse.source.browse.filter.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.activeCount
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterOptionChip
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation.displayMessage
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.NoResults
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The options of an open group, narrowed by its search and by the Selected only or Changed views.
 *
 * Groups made only of on/off and include/exclude options render as a chip grid; any other group keeps full rows,
 * drawn by [item] with the remaining focus path for the child the sheet was opened at.
 */
@Composable
internal fun GroupFilterBody(
    filter: EntryFilter.Group<*>,
    state: FilterGroupUiState,
    changes: FilterChanges,
    changedOnly: Boolean,
    focus: List<Int>?,
    onUpdate: () -> Unit,
    item: @Composable (child: EntryFilter<*>, childFocus: List<Int>?) -> Unit,
) {
    val children = filter.state.filterIsInstance<EntryFilter<*>>()
    val matching = if (filter.isSearchable()) children.filterGroupOptions(state.query) else children
    val visible = when {
        changedOnly -> matching.filter { changes[it].isChanged || it.validationIssues().isNotEmpty() }
        state.selectedOnly -> matching.filter { it.activeCount() != 0 }
        else -> matching
    }
    val focusedChild = focus?.firstOrNull()?.let { children.getOrNull(it) }
    Column {
        if (visible.isEmpty()) {
            NoResults(
                message = stringResource(
                    if (state.query.isNotBlank()) MR.strings.no_results_found else MR.strings.browse_filter_no_selected,
                ),
            )
        } else if (children.all { it.isOptionToggle() }) {
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KatariTheme.spacing.rowInset),
                horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
            ) {
                visible.forEach { FilterOptionChip(it, onUpdate) }
            }
            if (children.any { it is EntryFilter.TriState }) {
                Text(
                    text = stringResource(MR.strings.filter_tristate_chip_hint),
                    modifier = Modifier.padding(
                        horizontal = KatariTheme.spacing.rowInset,
                        vertical = KatariTheme.spacing.xs,
                    ),
                    role = TextRole.Supporting,
                )
            }
        } else {
            visible.forEach { child -> item(child, focus?.drop(1)?.takeIf { child === focusedChild }) }
        }
        // Children list their own issues; the group lists only the issues it reports about them together.
        val childIssues = children.flatMapTo(HashSet()) { it.validationIssues() }
        filter.validationIssues().filterNot { it in childIssues }.forEach {
            Text(
                text = it.displayMessage(),
                modifier = Modifier.padding(
                    horizontal = KatariTheme.spacing.rowInset,
                    vertical = KatariTheme.spacing.xs,
                ),
                role = TextRole.Supporting,
                tone = ContentTone.Error,
            )
        }
    }
}

private fun EntryFilter<*>.isOptionToggle(): Boolean = this is EntryFilter.CheckBox || this is EntryFilter.TriState
