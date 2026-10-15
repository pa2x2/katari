package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.paging.LoadState
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.domain.source.model.snapshot
import eu.kanade.presentation.browse.CatalogContent
import eu.kanade.presentation.browse.MissingSourceScreen
import eu.kanade.presentation.browse.components.BrowseEntryPreviewSheet
import eu.kanade.presentation.browse.components.BrowseFeedNameDialog
import eu.kanade.presentation.browse.components.BrowseLibraryActionDialog
import eu.kanade.presentation.browse.components.BrowseMergeEditorDialog
import eu.kanade.presentation.browse.components.DeleteBrowsePresetDialog
import eu.kanade.presentation.browse.components.DuplicateDetectionLoadingDialog
import eu.kanade.presentation.browse.components.MergeTargetPickerDialog
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.browse.immersive.rememberEntryImmersivePositionState
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.entry.components.DuplicateEntryDialog
import eu.kanade.presentation.more.settings.screen.BrowseLongPressActionsScreen
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveViewModel
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.FilterChanges
import eu.kanade.tachiyomi.ui.browse.source.browse.filter.change.changeLabels
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.SourceFilterPresetActions
import eu.kanade.tachiyomi.ui.browse.source.browse.preset.displayName
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.feature.migration.dialog.MigrateEntryDialog
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.presentation.core.util.collectAsLazyPagingItems
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarHost
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.progress.PullRefresh
import mihon.presentation.kit.surface.HorizontalDivider
import mihon.presentation.kit.surface.Surface
import mihon.presentation.kit.surface.SurfaceContainer
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

data class CatalogScreen(
    val sourceId: Long,
    private val listingQuery: String?,
    /** Opens the filter sheet right away, for building a preset to add as a feed. */
    private val openFilters: Boolean = false,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val viewModel = assistedMetroViewModel<CatalogViewModel, CatalogViewModel.Factory> {
            create(
                sourceId = sourceId,
                listingQuery = listingQuery,
                migrationEntryType = null,
                initialFilterSnapshot = emptyList(),
                initialPresetId = null,
                openFilterSheetOnStart = openFilters,
                feedProfileId = null,
            )
        }
        val state by viewModel.state.collectAsState()
        val feedsEnabled = viewModel.feedsEnabled

        val navigator = LocalNavigator.currentOrThrow
        val navigateUp: () -> Unit = {
            when {
                !state.isUserQuery && state.toolbarQuery != null -> viewModel.setToolbarQuery(null)
                else -> navigator.pop()
            }
        }

        val catalogSource = viewModel.catalogSource
        if (catalogSource == null) {
            MissingSourceScreen(
                source = viewModel.sourceDisplayInfo,
                navigateUp = navigateUp,
            )
            return
        }

        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val snackbarHostState = remember { SnackbarHostState() }

        val catalogList = viewModel.catalogPagerFlowFlow.collectAsLazyPagingItems()
        CatalogScreenEventsEffect(viewModel, snackbarHostState)
        var presetPendingDeletion by rememberSaveable { mutableStateOf<String?>(null) }
        var filterFocusPath by remember { mutableStateOf<List<Int>?>(null) }
        var immersiveMode by rememberSaveable(sourceId) { mutableStateOf(false) }
        val immersivePositionState = rememberEntryImmersivePositionState(resetKey = state.listing)
        val immersiveAvailable = viewModel.isImmersiveSourceAvailable
        val immersiveModel = metroViewModel<EntryImmersiveViewModel>(key = "catalog-immersive-$sourceId")

        BackHandler(enabled = immersiveMode) { immersiveMode = false }

        LaunchedEffect(immersiveAvailable) {
            if (!immersiveAvailable) immersiveMode = false
        }

        val onWebViewClick = viewModel.homeUrl?.let { url ->
            {
                navigator.push(
                    WebViewScreen(
                        url = url,
                        initialTitle = viewModel.sourceName,
                        sourceId = sourceId,
                    ),
                )
            }
        }

        val onSettingsClick = if (viewModel.isConfigurable) {
            {
                navigator.push(SourcePreferencesScreen(sourceId))
            }
        } else {
            null
        }

        LaunchedEffect(feedsEnabled, state.dialog) {
            if (!feedsEnabled) {
                if (state.dialog is CatalogViewModel.Dialog.SavePreset) {
                    viewModel.dismissDialog()
                }
                presetPendingDeletion = null
            }
        }

        LaunchedEffect(viewModel.homeUrl) {
            assistUrl = viewModel.homeUrl
        }

        val appliedSearch = state.listing as? CatalogViewModel.Listing.Search ?: state.restorableSearch
        val appliedFilterChanges = remember(appliedSearch, state.filterRevision) {
            appliedSearch?.let { FilterChanges.of(it.filters, state.defaultFilters) } ?: FilterChanges.Empty
        }
        val appliedFilterLabels = remember(appliedSearch, state.filterRevision) {
            appliedSearch?.let { changeLabels(it.filters, state.defaultFilters) }.orEmpty()
        }

        if (immersiveMode) {
            CatalogImmersiveContent(
                catalogList = catalogList,
                immersiveModel = immersiveModel,
                sourceName = viewModel.sourceName,
                snackbarHostState = snackbarHostState,
                onExitImmersive = { immersiveMode = false },
                onEntryClick = { navigator.push(EntryScreen(it.id, fromSource = true)) },
                onLibraryAction = viewModel::confirmBrowseLibraryAction,
                positionState = immersivePositionState,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Scaffold(
                topBar = {
                    Surface(container = SurfaceContainer.Surface) {
                        Column {
                            CatalogToolbar(
                                title = viewModel.sourceName,
                                searchQuery = state.toolbarQuery,
                                onSearchQueryChange = viewModel::setToolbarQuery,
                                displayMode = viewModel.displayMode,
                                onDisplayModeChange = { viewModel.displayMode = it },
                                navigateUp = navigateUp,
                                onWebViewClick = onWebViewClick,
                                onSettingsClick = onSettingsClick,
                                onLongPressActionsClick = {
                                    navigator.push(BrowseLongPressActionsScreen(sourceId))
                                },
                                onSearch = viewModel::search,
                                onEnterImmersive = if (immersiveAvailable) {
                                    { immersiveMode = true }
                                } else {
                                    null
                                },
                            )

                            CatalogListingChips(
                                listing = state.listing,
                                supportsLatest = viewModel.supportsLatest,
                                showFilterChip = state.filters.isNotEmpty() || viewModel.hasFilterCapability,
                                changedFilterCount = appliedFilterChanges.total.changed,
                                onListingChange = viewModel::setListing,
                                onFilterChipClick = viewModel::onFilterChipClick,
                            )
                            if (state.listing is CatalogViewModel.Listing.Search && appliedFilterLabels.isNotEmpty()) {
                                CatalogAppliedFilterChips(
                                    labels = appliedFilterLabels,
                                    onOpenFilters = {
                                        filterFocusPath = it.path
                                        viewModel.openFilterSheet()
                                    },
                                    onRemove = { viewModel.removeAppliedFilter(it.path) },
                                )
                            }

                            HorizontalDivider()
                        }
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            ) { paddingValues ->
                PullRefresh(
                    refreshing = catalogList.itemCount > 0 && catalogList.loadState.refresh is LoadState.Loading,
                    enabled = catalogList.loadState.refresh !is LoadState.Loading,
                    onRefresh = catalogList::refresh,
                    modifier = Modifier.fillMaxSize(),
                    indicatorPadding = paddingValues,
                ) {
                    CatalogContent(
                        catalogList = catalogList,
                        columns = viewModel.getColumnsPreference(
                            LocalConfiguration.current.orientation,
                            viewModel.sourceItemOrientation,
                        ),
                        displayMode = viewModel.displayMode,
                        sourceItemOrientation = viewModel.sourceItemOrientation,
                        snackbarHostState = snackbarHostState,
                        contentPadding = paddingValues,
                        onItemClick = { item ->
                            val entryId = (item as CatalogListItem.EntryItem).entry.id
                            navigator.push(EntryScreen(entryId, fromSource = true))
                        },
                        onItemLongClick = { item ->
                            scope.launch {
                                val outcome = withIOContext {
                                    viewModel.onItemLongClick(item)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (outcome == BrowseLongPressOutcome.StartImmersive) {
                                    val selectedIndex = (0 until catalogList.itemCount).firstOrNull { index ->
                                        val candidate = catalogList.peek(index)?.value
                                        candidate?.id == item.id && candidate.entryType == item.entryType
                                    }
                                    selectedIndex?.let(immersivePositionState::updateItemIndex)
                                    immersiveMode = true
                                }
                            }
                        },
                        onWebViewClick = onWebViewClick,
                        onSettingsClick = onSettingsClick,
                        positionState = immersivePositionState,
                    )
                }
            }
        }

        val onDismissRequest = viewModel::dismissDialog
        val draftPreset = if (feedsEnabled) viewModel.draftPreset() else null
        val draftCustomPreset = if (feedsEnabled) viewModel.draftCustomPreset() else null
        when (val dialog = state.dialog) {
            is CatalogViewModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = {
                        filterFocusPath = null
                        onDismissRequest()
                    },
                    filters = state.filters,
                    defaultFilters = state.defaultFilters,
                    filterRevision = state.filterRevision,
                    isLoading = state.filterState is FilterUiState.Loading,
                    errorMessage = (state.filterState as? FilterUiState.Error)?.throwable?.message,
                    presetActions = if (feedsEnabled) {
                        SourceFilterPresetActions(
                            presets = viewModel.feedPresets(),
                            currentPresetId = draftPreset?.id,
                            currentPresetName = draftPreset?.displayName(),
                            onApply = viewModel::applyPreset,
                            onEdit = viewModel::showEditPresetDialog,
                            onDelete = { presetPendingDeletion = it },
                            canDelete = viewModel::canDeletePreset,
                            onSaveAsNew = viewModel::showSavePresetDialog,
                            onUpdateCurrent = draftCustomPreset?.let { viewModel::showUpdateCurrentPresetDialog },
                        )
                    } else {
                        null
                    },
                    focusPath = filterFocusPath,
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
                    hasUnappliedChanges = state.hasUnappliedFilterChanges,
                    onUpdate = viewModel::setFilters,
                    onRequestSuggestions = viewModel::filterSuggestions,
                    onRequestPagedFilterItems = viewModel::pagedFilterItems,
                    onRequestPagedFilterNavigation = viewModel::pagedFilterNavigation,
                    pagedFilterBrowseSession = viewModel::pagedFilterBrowseSession,
                    onRetry = viewModel::retryFilterLoad,
                )
            }
            is CatalogViewModel.Dialog.SavePreset -> if (feedsEnabled) {
                BrowseFeedNameDialog(
                    title = when (dialog.mode) {
                        CatalogViewModel.Dialog.SavePreset.Mode.Create -> MR.strings.browse_feed_save_preset
                        CatalogViewModel.Dialog.SavePreset.Mode.EditMetadata -> MR.strings.action_edit
                        CatalogViewModel.Dialog.SavePreset.Mode.UpdateFromCurrentState ->
                            MR.strings.browse_feed_update_preset
                    },
                    initialValue = dialog.name,
                    initialChronological = dialog.chronological,
                    duplicateName = { name ->
                        viewModel.hasPresetName(name, excludingPresetId = dialog.presetId)
                    },
                    onDismissRequest = onDismissRequest,
                    onConfirm = viewModel::savePreset,
                )
            }
            is CatalogViewModel.Dialog.EntryPreview -> {
                BrowseEntryPreviewSheet(
                    entryId = dialog.entryId,
                    onLibraryAction = viewModel::confirmBrowseLibraryAction,
                    onMergeAction = viewModel::showMergeTargetPicker,
                    onOpenEntry = {
                        onDismissRequest()
                        navigator.push(EntryScreen(it, fromSource = true))
                    },
                    onDismissRequest = onDismissRequest,
                )
            }
            is CatalogViewModel.Dialog.LibraryActionChooser -> {
                BrowseLibraryActionDialog(
                    mangaTitle = dialog.entry.displayTitle,
                    favorite = dialog.entry.favorite,
                    onDismissRequest = onDismissRequest,
                    onLibraryAction = {
                        onDismissRequest()
                        viewModel.confirmBrowseLibraryAction(
                            CatalogListItem.EntryItem(dialog.entry, viewModel.sourceItemOrientation),
                        )
                    },
                    onMergeIntoLibrary = {
                        viewModel.showMergeTargetPicker(
                            CatalogListItem.EntryItem(dialog.entry, viewModel.sourceItemOrientation),
                        )
                    },
                )
            }
            CatalogViewModel.Dialog.CheckingDuplicates -> DuplicateDetectionLoadingDialog()
            is CatalogViewModel.Dialog.RemoveEntry -> {
                RemoveMangaDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        viewModel.changeFavorite(
                            CatalogListItem.EntryItem(dialog.entry, viewModel.sourceItemOrientation),
                        )
                    },
                    mangaToRemove = dialog.entry,
                )
            }
            is CatalogViewModel.Dialog.DuplicateEntry -> {
                DuplicateEntryDialog(
                    duplicates = dialog.duplicates,
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        viewModel.addFavorite(
                            CatalogListItem.EntryItem(dialog.entry, viewModel.sourceItemOrientation),
                        )
                    },
                    onOpenEntry = { navigator.push(EntryScreen(it.id, fromSource = true)) },
                    onMigrate = {
                        viewModel.showMigrateEntryDialog(current = it, target = dialog.entry)
                    },
                )
            }
            is CatalogViewModel.Dialog.SelectEntryMergeTarget -> {
                MergeTargetPickerDialog(
                    title = stringResource(MR.strings.action_merge_into_library),
                    query = dialog.query,
                    visibleTargets = dialog.visibleTargets,
                    onDismissRequest = onDismissRequest,
                    onQueryChange = viewModel::updateMergeTargetQuery,
                    onSelectTarget = viewModel::openMergeEditor,
                )
            }
            is CatalogViewModel.Dialog.EditEntryMerge -> {
                BrowseMergeEditorDialog(
                    entries = dialog.entries,
                    targetId = dialog.targetId,
                    targetLocked = dialog.targetLocked,
                    removedIds = dialog.removedIds,
                    libraryRemovalIds = dialog.libraryRemovalIds,
                    confirmEnabled = dialog.enabled,
                    onDismissRequest = onDismissRequest,
                    onMove = viewModel::moveMergeEntry,
                    onSelectTarget = viewModel::setMergeTarget,
                    onToggleRemove = viewModel::toggleMergeEntryRemoval,
                    onToggleLibraryRemove = viewModel::toggleMergeEntryLibraryRemoval,
                    onConfirm = viewModel::confirmBrowseMerge,
                )
            }
            is CatalogViewModel.Dialog.ChangeEntryCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        viewModel.addFavorite(dialog.entry, include)
                    },
                )
            }
            is CatalogViewModel.Dialog.MigrateEntry -> {
                MigrateEntryDialog(
                    current = dialog.current,
                    target = dialog.target,
                    onClickTitle = { navigator.push(EntryScreen(dialog.current.id, fromSource = true)) },
                    onDismissRequest = onDismissRequest,
                    onComplete = {
                        viewModel.dismissDialog()
                        navigator.push(EntryScreen(dialog.target.id, fromSource = true))
                    },
                )
            }
            null -> {}
        }

        presetPendingDeletion
            ?.takeIf { feedsEnabled }
            ?.let { presetId -> viewModel.feedPresets().firstOrNull { it.id == presetId } }
            ?.let { preset ->
                DeleteBrowsePresetDialog(
                    presetName = preset.displayName(),
                    onDismissRequest = { presetPendingDeletion = null },
                    onConfirm = { viewModel.removePreset(preset.id) },
                )
            }

        LaunchedEffect(Unit) {
            queryEvent.receiveAsFlow()
                .collectLatest {
                    when (it) {
                        is SearchType.Genre -> viewModel.searchGenre(it.txt)
                        is SearchType.Text -> viewModel.search(it.txt)
                    }
                }
        }
    }

    suspend fun search(query: String) = queryEvent.send(SearchType.Text(query))
    suspend fun searchGenre(name: String) = queryEvent.send(SearchType.Genre(name))

    companion object {
        private val queryEvent = Channel<SearchType>()
    }

    sealed class SearchType(val txt: String) {
        class Text(txt: String) : SearchType(txt)
        class Genre(txt: String) : SearchType(txt)
    }
}
