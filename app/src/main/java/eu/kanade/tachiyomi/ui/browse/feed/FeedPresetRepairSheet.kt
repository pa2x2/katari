package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreenModel
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun FeedPresetRepairNotice(onRepair: () -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(stringResource(MR.strings.filter_feed_repair), style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onRepair) { Text(stringResource(MR.strings.filter_repair_title)) }
    }
}

@Composable
internal fun FeedPresetRepairSheet(screenModel: CatalogScreenModel, state: CatalogScreenModel.State) {
    SourceFilterDialog(
        onDismissRequest = screenModel::dismissDialog,
        filters = state.filters,
        filterRevision = state.filterRevision,
        presets = emptyList(),
        currentPresetName = screenModel.draftCustomPreset()?.name,
        onReset = screenModel::resetFilters,
        onResetGroup = screenModel::resetFilterGroup,
        pendingFilterEdits = state.pendingFilterEdits,
        draftQuery = state.draftSearchQuery,
        onEditPagedItem = screenModel::editPagedFilterItem,
        onApplyPreset = {},
        onEditPreset = {},
        onDeletePreset = {},
        canDeletePreset = { false },
        onFilter = screenModel::applyDraftFilters,
        repairIssues = state.repairIssues,
        repairNeedsSave = state.repairNeedsSave,
        onResolveIssue = screenModel::resolvePresetIssue,
        onSaveRepair = screenModel::saveRepairedPreset,
        onUpdate = screenModel::setFilters,
        onRequestSuggestions = screenModel::filterSuggestions,
        onRequestPagedFilterItems = screenModel::pagedFilterItems,
        onRequestPagedFilterNavigation = screenModel::pagedFilterNavigation,
        pagedFilterBrowseSession = screenModel::pagedFilterBrowseSession,
    )
}
