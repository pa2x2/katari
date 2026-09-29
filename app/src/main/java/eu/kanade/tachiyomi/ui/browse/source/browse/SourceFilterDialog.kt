package eu.kanade.tachiyomi.ui.browse.source.browse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.paging.PagingSource
import eu.kanade.domain.source.model.FilterRestoreIssue
import eu.kanade.presentation.components.AdaptiveSheet
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageLoadReason
import eu.kanade.tachiyomi.source.entry.EntryFilterPageScope
import eu.kanade.tachiyomi.source.entry.EntryFilterTextInput
import eu.kanade.tachiyomi.source.entry.filter.validationIssues
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.date.DateFilterEditorHost
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.date.DateFilterEditorSession
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.group.FilterGroupUiStates
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.paged.PagedFilterBrowseSession
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.paged.PagedGroupFilterContent
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation.FilterValidation
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation.FilterValidationBar
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation.LocalFilterValidation
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.validation.filterApplyBlockedReason
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.SourceFilterPresetActions
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.SourceFilterPresetChip
import mihon.entry.interactions.catalogue.EntryCatalogueFilterNavigationResult
import mihon.entry.interactions.catalogue.EntryCatalogueFilterSuggestionsResult
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance

@Composable
fun SourceFilterDialog(
    onDismissRequest: () -> Unit,
    filters: EntryFilterList,
    defaultFilters: EntryFilterList,
    filterRevision: Int = 0,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    presetActions: SourceFilterPresetActions? = null,
    focusPath: List<Int>? = null,
    onReset: () -> Unit,
    onResetGroup: (EntryFilter<*>) -> Unit,
    pendingFilterEdits: Int,
    onEditPagedItem: (EntryFilter.PagedGroup<*>, EntryFilterPageItem, EntryFilter<*>, (Boolean) -> Unit) -> Unit,
    draftQuery: String? = null,
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
    var route by rememberSaveable(stateSaver = sourceFilterRouteSaver()) {
        mutableStateOf<SourceFilterRoute>(SourceFilterRoute.Root)
    }
    var changedOnly by rememberSaveable { mutableStateOf(false) }
    // The sheet reveals the focused filter once when it opens, not every time the root page is shown again.
    var focusRevealed by rememberSaveable { mutableStateOf(false) }
    val focus = focusPath?.takeUnless { focusRevealed }
    val validation = filters.validationIssues()
    val filterValidation = FilterValidation(validation)
    val changes = FilterChanges.of(filters, defaultFilters)
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
        if (onFilter()) onDismissRequest() else changedOnly = false
    }

    val canSaveDraft = !isLoading && !isError && pendingFilterEdits == 0 && validation.isEmpty() &&
        repairIssues.isEmpty()
    val groupStates = rememberSaveable(saver = FilterGroupUiStates.Saver) {
        FilterGroupUiStates(expandedIndex = focusPath?.firstOrNull())
    }
    // Top-level filter the root page should scroll to once it is shown, after a Show on the validation bar.
    var pendingReveal by remember { mutableStateOf<Int?>(null) }
    val showFirstProblem: () -> Unit = {
        filterValidation.firstInvalidIndex(filters)?.let { index ->
            if (filters[index] is EntryFilter.Group<*>) groupStates.of(index).expanded = true
            pendingReveal = index
            route = SourceFilterRoute.Root
        }
    }
    val validationBar: @Composable () -> Unit = { FilterValidationBar(filterValidation, onShow = showFirstProblem) }

    BackHandler(enabled = route is SourceFilterRoute.PagedGroup, onBack = leavePagedGroup)

    AdaptiveSheet(
        onDismissRequest = dismissOrLeavePagedGroup,
        enableImplicitDismiss = dateEditor.editing == null && route is SourceFilterRoute.Root,
        modifier = if (route is SourceFilterRoute.PagedGroup) Modifier.fillMaxHeight(0.9f) else Modifier,
    ) {
        DateFilterEditorHost(dateEditor) {
            CompositionLocalProvider(LocalFilterValidation provides filterValidation) {
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
                            val rows = sourceFilterRows(
                                filters = filters,
                                isLoading = isLoading,
                                errorMessage = errorMessage,
                                validation = validation,
                                repairIssues = repairIssues,
                                repairNeedsSave = repairNeedsSave,
                                hasPendingEdits = pendingFilterEdits > 0,
                                changes = changes,
                                changedOnly = changedOnly,
                                isGroupExpanded = { groupStates.of(it).expanded },
                            )
                            LaunchedEffect(Unit) {
                                val focusIndex = focus?.firstOrNull() ?: return@LaunchedEffect
                                val row = rows.indexOfFirst { it is SourceFilterRow.Filter && it.index == focusIndex }
                                if (row >= 0) rootListState.scrollToItem(row)
                                focusRevealed = true
                            }
                            LaunchedEffect(pendingReveal) {
                                val index = pendingReveal ?: return@LaunchedEffect
                                val row = rows.indexOfFirst { it is SourceFilterRow.Filter && it.index == index }
                                if (row >= 0) rootListState.animateScrollToItem(row)
                                pendingReveal = null
                            }
                            Column(Modifier.fillMaxHeight(0.9f)) {
                                SourceFilterRootHeader(
                                    status = SourceFilterSheetStatus(
                                        changedCount = changes.total.changed,
                                        updating = pendingFilterEdits > 0,
                                        unapplied = hasUnappliedChanges,
                                        query = draftQuery,
                                    ),
                                    changedOnly = changedOnly,
                                    onChangedOnlyChange = { changedOnly = it },
                                    presetChip = presetActions?.let { actions ->
                                        { SourceFilterPresetChip(actions, canSave = canSaveDraft) }
                                    },
                                )
                                validationBar()
                                SourceFilterRootList(
                                    rows = rows,
                                    listState = rootListState,
                                    filters = filters,
                                    changes = changes,
                                    groupStates = groupStates,
                                    focus = focus,
                                    onUpdate = updateFilters,
                                    onOpenPagedGroup = {
                                        filters.pathTo(it)?.let { path -> route = SourceFilterRoute.PagedGroup(path) }
                                    },
                                    onRequestSuggestions = onRequestSuggestions,
                                    onResetGroup = onResetGroup,
                                    onRetry = onRetry,
                                    onSaveRepair = onSaveRepair,
                                    onResolveIssue = onResolveIssue,
                                    onShowAll = { changedOnly = false },
                                    modifier = Modifier.weight(1f),
                                )
                                SourceFilterSheetFooter(
                                    onReset = onReset,
                                    resetEnabled = !isLoading,
                                    onApply = filterAndDismiss,
                                    applyEnabled = canSaveDraft && !repairNeedsSave,
                                    applyBlockedReason = filterApplyBlockedReason(filterValidation, repairNeedsSave),
                                )
                            }
                        }
                        is SourceFilterRoute.PagedGroup -> {
                            val liveFilter = filters.resolvePagedGroup(currentRoute.path)
                            if (liveFilter == null) {
                                LaunchedEffect(currentRoute.path) {
                                    leavePagedGroup()
                                }
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                PagedGroupFilterContent(
                                    filter = liveFilter,
                                    filterRevision = filterRevision,
                                    onBack = leavePagedGroup,
                                    onReset = { onResetGroup(liveFilter) },
                                    onEditItem = { item, value, complete ->
                                        onEditPagedItem(liveFilter, item, value, complete)
                                    },
                                    onRequestSuggestions = onRequestSuggestions,
                                    onRequestNavigation = { scope, query ->
                                        onRequestPagedFilterNavigation(liveFilter, scope, query)
                                    },
                                    browseSession = pagedFilterBrowseSession(liveFilter),
                                    pagingSourceFactory = { scope, query, reason, initialAnchor ->
                                        onRequestPagedFilterItems(
                                            liveFilter,
                                            scope,
                                            query,
                                            reason,
                                            initialAnchor,
                                        )
                                    },
                                    banner = validationBar,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
