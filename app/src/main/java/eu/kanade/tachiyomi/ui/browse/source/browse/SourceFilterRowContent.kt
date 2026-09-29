package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.domain.source.model.FilterRestoreIssue
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.FilterPresetRepairItem
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.control.FilterSheetInsets
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.displayMessage
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Renders a planned root row; filters themselves are drawn by [filterItem]. */
@Composable
internal fun SourceFilterRowContent(
    row: SourceFilterRow,
    filters: EntryFilterList,
    onRetry: (() -> Unit)?,
    onSaveRepair: () -> Unit,
    onResolveIssue: (FilterRestoreIssue, Boolean) -> Unit,
    onShowAll: () -> Unit,
    filterItem: @Composable (SourceFilterRow.Filter) -> Unit,
    groupTools: @Composable (SourceFilterRow.GroupTools) -> Unit,
    groupBody: @Composable (SourceFilterRow.GroupBody) -> Unit,
    repairFilterItem: @Composable (EntryFilter<*>) -> Unit,
) {
    val blockModifier = Modifier.padding(FilterSheetInsets.Horizontal)
    when (row) {
        SourceFilterRow.Loading -> Text(stringResource(MR.strings.loading), modifier = blockModifier)
        is SourceFilterRow.LoadError -> Column(blockModifier) {
            Text(row.message)
            onRetry?.let {
                TextButton(onClick = it) { Text(stringResource(MR.strings.action_retry)) }
            }
        }
        is SourceFilterRow.RepairIntro -> Column(blockModifier) {
            Text(stringResource(MR.strings.filter_repair_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(MR.strings.filter_repair_help), style = MaterialTheme.typography.bodyMedium)
            if (row.showSave) {
                TextButton(enabled = row.saveEnabled, onClick = onSaveRepair) {
                    Text(stringResource(MR.strings.filter_save_repair))
                }
            }
        }
        is SourceFilterRow.RepairIssue -> FilterPresetRepairItem(row.issue, filters, onResolveIssue, repairFilterItem)
        is SourceFilterRow.ValidationSummary -> Column(blockModifier) {
            Text(stringResource(MR.strings.filter_validation_help), color = MaterialTheme.colorScheme.error)
            row.issues.forEach {
                Text(
                    it.displayMessage(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        SourceFilterRow.NoChanges -> Column(blockModifier) {
            Text(stringResource(MR.strings.filter_no_changes))
            TextButton(onClick = onShowAll) { Text(stringResource(MR.strings.filter_show_all)) }
        }
        SourceFilterRow.OrderingDivider -> HorizontalDivider(
            Modifier.padding(horizontal = FilterSheetInsets.Horizontal, vertical = 4.dp),
        )
        is SourceFilterRow.Filter -> filterItem(row)
        is SourceFilterRow.GroupTools -> groupTools(row)
        is SourceFilterRow.GroupBody -> groupBody(row)
        is SourceFilterRow.GroupEnd -> Unit
    }
}
