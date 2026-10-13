package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogViewModel
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.SourceFilterPresetActions
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun FeedPresetRepairNotice(onRepair: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(MR.strings.filter_feed_repair), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRepair) { Text(stringResource(MR.strings.filter_repair_title)) }
    }
}

@Composable
internal fun FeedPresetRepairSheet(viewModel: CatalogViewModel, state: CatalogViewModel.State) {
    SourceFilterDialog(
        onDismissRequest = viewModel::dismissDialog,
        filters = state.filters,
        defaultFilters = state.defaultFilters,
        filterRevision = state.filterRevision,
        presetActions = viewModel.draftCustomPreset()?.let {
            SourceFilterPresetActions(
                presets = emptyList(),
                currentPresetId = it.id,
                currentPresetName = it.displayName(),
            )
        },
        onReset = viewModel::resetFilters,
        onResetGroup = viewModel::resetFilterGroup,
        pendingFilterEdits = state.pendingFilterEdits,
        draftQuery = state.draftSearchQuery,
        onEditPagedItem = viewModel::editPagedFilterItem,
        onFilter = viewModel::applyDraftFilters,
        repairIssues = state.repairIssues,
        repairNeedsSave = state.repairNeedsSave,
        onResolveIssue = viewModel::resolvePresetIssue,
        onSaveRepair = viewModel::saveRepairedPreset,
        onUpdate = viewModel::setFilters,
        onRequestSuggestions = viewModel::filterSuggestions,
        onRequestPagedFilterItems = viewModel::pagedFilterItems,
        onRequestPagedFilterNavigation = viewModel::pagedFilterNavigation,
        pagedFilterBrowseSession = viewModel::pagedFilterBrowseSession,
    )
}
