package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.FilterRestoreIssue
import eu.kanade.domain.source.model.FilterStateNode
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.SelectItem
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun FilterPresetRepairItem(
    issue: FilterRestoreIssue,
    filters: EntryFilterList,
    onResolve: (FilterRestoreIssue, Boolean) -> Unit,
    item: @Composable (EntryFilter<*>) -> Unit,
) {
    fun leaves(values: List<EntryFilter<*>>): List<EntryFilter<*>> = values.flatMap {
        if (it is EntryFilter.Group<*>) leaves(it.state.filterIsInstance<EntryFilter<*>>()) else listOf(it)
    }
    val candidates = leaves(filters).filter { it.accepts(issue.saved) }
    var selected by remember(issue) { mutableStateOf(issue.target) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(issue.saved.name, style = MaterialTheme.typography.titleSmall)
            Text(
                issue.saved.savedValueLabel(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (issue.target == null && candidates.isNotEmpty()) {
                SelectItem(
                    stringResource(MR.strings.filter_replacement),
                    arrayOf(stringResource(MR.strings.filter_choose_replacement)) + candidates.map {
                        it.name
                    },
                    (
                        candidates.indexOf(selected) +
                            1
                        ).coerceAtLeast(0),
                ) {
                    selected = candidates.getOrNull(it - 1)
                }
            }
            selected?.let { item(it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(enabled = selected != null && selected!!.validationIssues().isEmpty(), onClick = {
                    onResolve(issue, false)
                }) { Text(stringResource(MR.strings.filter_use_value)) }
                TextButton(onClick = {
                    onResolve(issue, true)
                }) { Text(stringResource(MR.strings.filter_remove_value)) }
            }
        }
    }
}

private fun EntryFilter<*>.accepts(node: FilterStateNode): Boolean = when (node) {
    is FilterStateNode.Text -> this is EntryFilter.Text
    is FilterStateNode.Select -> this is EntryFilter.Select<*>
    is FilterStateNode.CheckBox -> this is EntryFilter.CheckBox
    is FilterStateNode.TriState -> this is EntryFilter.TriState
    is FilterStateNode.Sort -> this is EntryFilter.Sort
    is FilterStateNode.PagedGroup -> this is EntryFilter.PagedGroup<*>
    else -> false
}

@Composable
private fun FilterStateNode.savedValueLabel(): String = when (this) {
    is FilterStateNode.Text -> state
    is FilterStateNode.Select -> identity?.optionId?.let {
        stringResource(MR.strings.filter_saved_choice, it)
    } ?: stringResource(MR.strings.filter_legacy_choice, state + 1)
    is FilterStateNode.CheckBox -> stringResource(
        if (state) MR.strings.filter_included else MR.strings.filter_neutral,
    )
    is FilterStateNode.TriState -> stringResource(
        when (state) {
            EntryFilter.TriState.STATE_INCLUDE -> MR.strings.filter_included
            EntryFilter.TriState.STATE_EXCLUDE -> MR.strings.filter_excluded
            else -> MR.strings.filter_neutral
        },
    )
    is FilterStateNode.Sort -> identity?.optionId?.let {
        stringResource(MR.strings.filter_saved_choice, it)
    } ?: stringResource(MR.strings.filter_saved_unsupported)
    else -> stringResource(MR.strings.filter_saved_unsupported)
}
