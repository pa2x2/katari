package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogViewModel
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.SourceFilterPresetActions
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun FeedPresetRepairNotice(onRepair: () -> Unit) {
    Column(Modifier.padding(horizontal = KatariTheme.spacing.contentInset)) {
        Text(stringResource(MR.strings.filter_feed_repair), role = TextRole.Body)
        Button(label = stringResource(MR.strings.filter_repair_title), onClick = onRepair, style = ButtonStyle.Text)
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
