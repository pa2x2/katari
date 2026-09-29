package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.FilterRestoreIssue
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterValidationIssue
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.FilterPresetRepairItem
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.displayMessage
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.isOrdering
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.i18n.stringResource

internal fun LazyListScope.sourceFilterFields(
    filters: EntryFilterList,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: (() -> Unit)?,
    validation: List<EntryFilterValidationIssue>,
    repairIssues: List<FilterRestoreIssue>,
    repairNeedsSave: Boolean,
    onSaveRepair: () -> Unit,
    hasPendingEdits: Boolean,
    onResolveIssue: (FilterRestoreIssue, Boolean) -> Unit,
    changes: FilterChanges,
    changedOnly: Boolean,
    onShowAll: () -> Unit,
    filterItem: @Composable (EntryFilter<*>, Boolean) -> Unit,
) {
    if (isLoading) {
        item { HeadingItem(stringResource(MR.strings.loading)) }
    } else if (errorMessage != null) {
        item {
            Column(Modifier.padding(16.dp)) {
                Text(errorMessage)
                onRetry?.let {
                    TextButton(onClick = it) { Text(stringResource(MR.strings.action_retry)) }
                }
            }
        }
    } else {
        if (repairIssues.isNotEmpty() || repairNeedsSave) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(MR.strings.filter_repair_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(MR.strings.filter_repair_help),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (repairIssues.isEmpty()) {
                        TextButton(
                            enabled = !hasPendingEdits && validation.isEmpty(),
                            onClick = onSaveRepair,
                        ) {
                            Text(stringResource(MR.strings.filter_save_repair))
                        }
                    }
                }
            }
            items(repairIssues) { issue ->
                FilterPresetRepairItem(issue, filters, onResolveIssue) { filter ->
                    filterItem(filter, false)
                }
            }
        }
        if (validation.isNotEmpty()) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(MR.strings.filter_validation_help),
                        color = MaterialTheme.colorScheme.error,
                    )
                    validation.forEach {
                        Text(
                            it.displayMessage(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        val shown = filters.filter { !changedOnly || changes[it].isChanged || it.validationIssues().isNotEmpty() }
        val ordering = shown.filter { it.isOrdering }
        if (ordering.isNotEmpty()) {
            item {
                HeadingItem(stringResource(MR.strings.filter_sorting))
            }
        }
        items(ordering) { filter ->
            filterItem(filter, false)
        }
        val constraints = shown.filterNot { it.isOrdering }
        if (changedOnly && shown.isEmpty()) {
            item {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(MR.strings.filter_no_changes))
                    TextButton(onClick = onShowAll) { Text(stringResource(MR.strings.filter_show_all)) }
                }
            }
        }
        items(constraints) { filter ->
            filterItem(filter, changedOnly)
        }
    }
}
