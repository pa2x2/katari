package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.PagingSource
import eu.kanade.domain.source.model.FilterRestoreIssue
import eu.kanade.domain.source.model.SourceFeedPreset
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageLoadReason
import eu.kanade.tachiyomi.source.entry.EntryFilterPageScope
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.FilterItem
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.FilterPresetRepairItem
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.PagedFilterBrowseSession
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.PagedGroupFilterContent
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.activeCount
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.date.DateFilterEditorHost
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.date.DateFilterEditorSession
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.displayMessage
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.isOrdering
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.resetFilterToDefault
import mihon.entry.interactions.catalogue.EntryCatalogueFilterNavigationResult
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.HeadingItem
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun SourceFilterDialog(
    onDismissRequest: () -> Unit,
    filters: EntryFilterList,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    presets: List<SourceFeedPreset>,
    onReset: () -> Unit,
    onResetGroup: (EntryFilter<*>) -> Unit,
    pendingFilterEdits: Int,
    onEditPagedItem: (EntryFilter.PagedGroup<*>, EntryFilterPageItem, EntryFilter<*>, (Boolean) -> Unit) -> Unit,
    onApplyPreset: (String) -> Unit,
    onEditPreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    canDeletePreset: (String) -> Boolean,
    onSaveAsNewPreset: (() -> Unit)? = null,
    currentPresetName: String? = null,
    draftQuery: String? = null,
    onUpdateCurrentPreset: (() -> Unit)? = null,
    onFilter: () -> Boolean,
    onUpdate: (EntryFilterList) -> Unit,
    onRequestSuggestions: suspend (
        EntryFilter.Autocomplete,
        EntryFilterTextInput,
    ) -> EntryCatalogueFilterSuggestionsResult,
    onRequestPagedFilterItems: (
        EntryFilter.PagedGroup<*>,
        EntryFilterPageScope,
        String?,
        EntryFilterPageLoadReason,
        String?,
    ) -> PagingSource<String, EntryFilterPageItem>,
    onRequestPagedFilterNavigation: suspend (
        EntryFilter.PagedGroup<*>,
        EntryFilterPageScope,
        String?,
    ) -> EntryCatalogueFilterNavigationResult,
    pagedFilterBrowseSession: (EntryFilter.PagedGroup<*>) -> PagedFilterBrowseSession,
    onRetry: (() -> Unit)? = null,
    repairIssues: List<FilterRestoreIssue> = emptyList(),
    repairNeedsSave: Boolean = false,
    hasUnappliedChanges: Boolean = false,
    onResolveIssue: (FilterRestoreIssue, Boolean) -> Unit = { _, _ -> },
    onSaveRepair: () -> Unit = {},
) {
    val dateEditor = remember { DateFilterEditorSession() }
    val updateFilters = { onUpdate(filters) }
    val rootListState = rememberLazyListState()
    var route by rememberSaveable(stateSaver = sourceFilterRouteSaver(filters)) {
        mutableStateOf<SourceFilterRoute>(SourceFilterRoute.Root)
    }
    var activeOnly by rememberSaveable { mutableStateOf(false) }
    val validation = filters.validationIssues()
    val isError = errorMessage != null
    val slideDistance = rememberSlideDistance()
    val leavePagedGroup = { route = SourceFilterRoute.Root }
    val dismissOrLeavePagedGroup = {
        if (dateEditor.editing != null) {
            dateEditor.cancel()
        } else if (route is SourceFilterRoute.PagedGroup) {
            leavePagedGroup()
        } else {
            onDismissRequest()
        }
    }
    val filterAndDismiss = {
        if (onFilter()) onDismissRequest() else activeOnly = false
    }

    BackHandler(enabled = route is SourceFilterRoute.PagedGroup, onBack = leavePagedGroup)

    AdaptiveSheet(
        onDismissRequest = dismissOrLeavePagedGroup,
        enableImplicitDismiss = dateEditor.editing == null && route is SourceFilterRoute.Root,
        modifier = if (route is SourceFilterRoute.PagedGroup) Modifier.fillMaxHeight(0.9f) else Modifier,
    ) {
        DateFilterEditorHost(dateEditor) {
            AnimatedContent(
                targetState = route,
                transitionSpec = {
                    materialSharedAxisX(
                        forward = targetState is SourceFilterRoute.PagedGroup,
                        slideDistance = slideDistance,
                    )
                },
                modifier = if (route is SourceFilterRoute.PagedGroup) Modifier.fillMaxSize() else Modifier,
                label = "sourceFilterRoute",
            ) { currentRoute ->
                when (currentRoute) {
                    SourceFilterRoute.Root -> {
                        Column(Modifier.fillMaxHeight(0.9f).imePadding()) {
                            Text(
                                stringResource(MR.strings.filter_title),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                            )
                            currentPresetName?.let {
                                Text(
                                    stringResource(MR.strings.filter_preset_name, it),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            draftQuery?.takeIf { it.isNotBlank() }?.let {
                                Text(
                                    stringResource(MR.strings.filter_query, it),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(
                                Modifier.padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterChip(selected = !activeOnly, onClick = {
                                    activeOnly = false
                                }, label = { Text(stringResource(MR.strings.filter_all)) })
                                FilterChip(selected = activeOnly, onClick = {
                                    activeOnly = true
                                }, label = { Text(stringResource(MR.strings.filter_active)) })
                            }
                            if (pendingFilterEdits > 0) {
                                Text(
                                    stringResource(MR.strings.filter_updating),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                            if (hasUnappliedChanges) {
                                Text(
                                    stringResource(MR.strings.filter_unapplied),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                )
                            }
                            LazyColumn(state = rootListState, modifier = Modifier.weight(1f)) {
                                sourceFilterFields(
                                    filters = filters,
                                    isLoading = isLoading,
                                    errorMessage = errorMessage,
                                    onRetry = onRetry,
                                    validation = validation,
                                    repairIssues = repairIssues,
                                    repairNeedsSave = repairNeedsSave,
                                    onSaveRepair = onSaveRepair,
                                    hasPendingEdits = pendingFilterEdits > 0,
                                    onResolveIssue = onResolveIssue,
                                    activeOnly = activeOnly,
                                    onShowAll = { activeOnly = false },
                                ) { filter, selectedOnly ->
                                    FilterItem(filter, updateFilters, {
                                        route = SourceFilterRoute.PagedGroup(it)
                                    }, onRequestSuggestions, selectedOnly) {
                                        onResetGroup(it)
                                    }
                                }
                            }
                            SourceFilterRootFooter(
                                presets = presets,
                                onReset = onReset,
                                onApplyPreset = onApplyPreset,
                                onEditPreset = onEditPreset,
                                onDeletePreset = onDeletePreset,
                                canDeletePreset = canDeletePreset,
                                onSaveAsNewPreset = onSaveAsNewPreset?.takeIf {
                                    !isLoading && !isError && pendingFilterEdits == 0 && validation.isEmpty() &&
                                        repairIssues.isEmpty()
                                },
                                currentPresetName = currentPresetName,
                                onUpdateCurrentPreset = onUpdateCurrentPreset?.takeIf {
                                    !isLoading && !isError && pendingFilterEdits == 0 && validation.isEmpty() &&
                                        repairIssues.isEmpty()
                                },
                                onFilter = filterAndDismiss,
                                resetEnabled = !isLoading,
                                filterEnabled =
                                !isLoading && !isError && pendingFilterEdits == 0 && validation.isEmpty() &&
                                    repairIssues.isEmpty() &&
                                    !repairNeedsSave,
                            )
                        }
                    }
                    is SourceFilterRoute.PagedGroup -> {
                        PagedGroupFilterContent(
                            filter = currentRoute.filter,
                            onBack = leavePagedGroup,
                            onFilter = filterAndDismiss,
                            onReset = { onResetGroup(currentRoute.filter) },
                            canApply = !isLoading && !isError && pendingFilterEdits == 0 && validation.isEmpty() &&
                                repairIssues.isEmpty() && !repairNeedsSave,
                            onEditItem = { item, value, complete ->
                                onEditPagedItem(currentRoute.filter, item, value, complete)
                            },
                            onRequestSuggestions = onRequestSuggestions,
                            onRequestNavigation = { scope, query ->
                                onRequestPagedFilterNavigation(currentRoute.filter, scope, query)
                            },
                            browseSession = pagedFilterBrowseSession(currentRoute.filter),
                            pagingSourceFactory = { scope, query, reason, initialAnchor ->
                                onRequestPagedFilterItems(
                                    currentRoute.filter,
                                    scope,
                                    query,
                                    reason,
                                    initialAnchor,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
