package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.domain.source.model.FilterRestoreIssue
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.FilterPresetRepairItem
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
    val blockModifier = Modifier.padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.l)
    when (row) {
        SourceFilterRow.Loading -> Text(
            stringResource(MR.strings.loading),
            modifier = blockModifier,
            role = TextRole.Body,
        )
        is SourceFilterRow.LoadError -> Column(blockModifier) {
            Text(row.message, role = TextRole.Body)
            onRetry?.let {
                Button(label = stringResource(MR.strings.action_retry), onClick = it, style = ButtonStyle.Text)
            }
        }
        is SourceFilterRow.RepairIntro -> Column(blockModifier) {
            Text(stringResource(MR.strings.filter_repair_title), role = TextRole.SectionTitle)
            Text(stringResource(MR.strings.filter_repair_help), role = TextRole.Body)
            if (row.showSave) {
                Button(
                    label = stringResource(MR.strings.filter_save_repair),
                    onClick = onSaveRepair,
                    style = ButtonStyle.Text,
                    enabled = row.saveEnabled,
                )
            }
        }
        is SourceFilterRow.RepairIssue -> FilterPresetRepairItem(row.issue, filters, onResolveIssue, repairFilterItem)
        SourceFilterRow.NoChanges -> Column(blockModifier) {
            Text(stringResource(MR.strings.filter_no_changes), role = TextRole.Body)
            Button(label = stringResource(MR.strings.filter_show_all), onClick = onShowAll, style = ButtonStyle.Text)
        }
        SourceFilterRow.OrderingDivider -> HorizontalDivider(
            Modifier.padding(horizontal = KatariTheme.spacing.rowInset, vertical = KatariTheme.spacing.xs),
        )
        is SourceFilterRow.Filter -> filterItem(row)
        is SourceFilterRow.GroupTools -> groupTools(row)
        is SourceFilterRow.GroupBody -> groupBody(row)
        is SourceFilterRow.GroupEnd -> Unit
    }
}
