package eu.kanade.tachiyomi.ui.browse.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.paging.compose.LazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
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
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.presentation.components.RadioMenuItem
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.entry.components.DuplicateEntryDialog
import eu.kanade.presentation.more.settings.screen.BrowseLongPressActionsScreen
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.browse.immersive.EntryImmersiveScreenModel
import eu.kanade.tachiyomi.ui.browse.immersive.ImmersiveSystemBarsEffect
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.feature.migration.dialog.MigrateEntryDialog
import mihon.presentation.core.util.collectAsLazyPagingItems
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen

data class CatalogScreen(
    val sourceId: Long,
    private val listingQuery: String?,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val screenModel = rememberScreenModel { CatalogScreenModel(sourceId, listingQuery) }
        val state by screenModel.state.collectAsState()
        val feedsEnabled = screenModel.feedsEnabled

        val navigator = LocalNavigator.currentOrThrow
        val navigateUp: () -> Unit = {
            when {
                !state.isUserQuery && state.toolbarQuery != null -> screenModel.setToolbarQuery(null)
                else -> navigator.pop()
            }
        }

        val catalogSource = screenModel.catalogSource
        if (catalogSource == null) {
            MissingSourceScreen(
                source = screenModel.sourceDisplayInfo,
                navigateUp = navigateUp,
            )
            return
        }

        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val snackbarHostState = remember { SnackbarHostState() }

        @Suppress("UNCHECKED_CAST")
        val catalogList =
            screenModel.catalogPagerFlowFlow.collectAsLazyPagingItems() as LazyPagingItems<StateFlow<CatalogListItem>>
        var presetPendingDeletion by rememberSaveable { mutableStateOf<String?>(null) }
        var immersiveMode by rememberSaveable(sourceId) { mutableStateOf(false) }
        val immersivePositionState = rememberEntryImmersivePositionState(resetKey = state.listing)
        val immersiveAvailable = screenModel.isImmersiveSourceAvailable
        val immersiveModel = rememberScreenModel(tag = "catalog-immersive-$sourceId") {
            EntryImmersiveScreenModel()
        }

        ImmersiveSystemBarsEffect(enabled = immersiveMode)
        BackHandler(enabled = immersiveMode) { immersiveMode = false }

        LaunchedEffect(immersiveAvailable) {
            if (!immersiveAvailable) immersiveMode = false
        }

        val onWebViewClick = screenModel.homeUrl?.let { url ->
            {
                navigator.push(
                    WebViewScreen(
                        url = url,
                        initialTitle = screenModel.sourceName,
                        sourceId = sourceId,
                    ),
                )
            }
        }

        val onSettingsClick = if (screenModel.isConfigurable) {
            {
                navigator.push(SourcePreferencesScreen(sourceId))
            }
        } else {
            null
        }

        LaunchedEffect(feedsEnabled, state.dialog) {
            if (!feedsEnabled) {
                if (state.dialog is CatalogScreenModel.Dialog.SavePreset) {
                    screenModel.dismissDialog()
                }
                presetPendingDeletion = null
            }
        }

        LaunchedEffect(screenModel.homeUrl) {
            assistUrl = screenModel.homeUrl
        }

        if (immersiveMode) {
            CatalogImmersiveContent(
                catalogList = catalogList,
                immersiveModel = immersiveModel,
                sourceName = screenModel.sourceName,
                snackbarHostState = snackbarHostState,
                onExitImmersive = { immersiveMode = false },
                onEntryClick = { navigator.push(EntryScreen(it.id, fromSource = true)) },
                onLibraryAction = screenModel::confirmBrowseLibraryAction,
                positionState = immersivePositionState,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
                    ) {
                        CatalogToolbar(
                            title = screenModel.sourceName,
                            searchQuery = state.toolbarQuery,
                            onSearchQueryChange = screenModel::setToolbarQuery,
                            displayMode = screenModel.displayMode,
                            onDisplayModeChange = { screenModel.displayMode = it },
                            navigateUp = navigateUp,
                            onWebViewClick = onWebViewClick,
                            onSettingsClick = onSettingsClick,
                            onLongPressActionsClick = {
                                navigator.push(BrowseLongPressActionsScreen(sourceId))
                            },
                            onSearch = screenModel::search,
                            onEnterImmersive = if (immersiveAvailable) {
                                { immersiveMode = true }
                            } else {
                                null
                            },
                        )

                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = MaterialTheme.padding.small),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        ) {
                            FilterChip(
                                selected = state.listing == CatalogScreenModel.Listing.Popular,
                                onClick = {
                                    screenModel.resetFilters()
                                    screenModel.setListing(CatalogScreenModel.Listing.Popular)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                },
                                label = { Text(text = stringResource(MR.strings.popular)) },
                            )
                            if (screenModel.supportsLatest) {
                                FilterChip(
                                    selected = state.listing == CatalogScreenModel.Listing.Latest,
                                    onClick = {
                                        screenModel.resetFilters()
                                        screenModel.setListing(CatalogScreenModel.Listing.Latest)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.NewReleases,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                                        )
                                    },
                                    label = { Text(text = stringResource(MR.strings.latest)) },
                                )
                            }
                            if (state.filters.isNotEmpty() || screenModel.hasFilterCapability) {
                                FilterChip(
                                    selected = state.listing is CatalogScreenModel.Listing.Search,
                                    onClick = screenModel::openFilterSheet,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.FilterList,
                                            contentDescription = null,
                                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                                        )
                                    },
                                    label = { Text(text = stringResource(MR.strings.action_filter)) },
                                )
                            }
                        }

                        HorizontalDivider()
                    }
                },
                snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
            ) { paddingValues ->
                if (state.isWaitingForInitialFilterLoad) {
                    when (val filterState = state.filterState) {
                        is FilterUiState.Error -> {
                            EmptyScreen(
                                message = filterState.throwable.message ?: stringResource(MR.strings.unknown_error),
                                modifier = Modifier.padding(paddingValues),
                            )
                        }
                        else -> LoadingScreen(Modifier.padding(paddingValues))
                    }
                } else {
                    PullRefresh(
                        refreshing = catalogList.itemCount > 0 && catalogList.loadState.refresh is LoadState.Loading,
                        enabled = catalogList.loadState.refresh !is LoadState.Loading,
                        onRefresh = catalogList::refresh,
                        modifier = Modifier.fillMaxSize(),
                        indicatorPadding = paddingValues,
                    ) {
                        CatalogContent(
                            catalogList = catalogList,
                            columns = screenModel.getColumnsPreference(
                                LocalConfiguration.current.orientation,
                                screenModel.sourceItemOrientation,
                            ),
                            displayMode = screenModel.displayMode,
                            sourceItemOrientation = screenModel.sourceItemOrientation,
                            snackbarHostState = snackbarHostState,
                            contentPadding = paddingValues,
                            onItemClick = { item ->
                                val entryId = (item as CatalogListItem.EntryItem).entry.id
                                navigator.push(EntryScreen(entryId, fromSource = true))
                            },
                            onItemLongClick = { item ->
                                scope.launch {
                                    val outcome = withIOContext {
                                        screenModel.onItemLongClick(item)
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
        }

        val onDismissRequest = screenModel::dismissDialog
        val appliedCustomPreset = if (feedsEnabled) screenModel.appliedCustomPreset() else null
        when (val dialog = state.dialog) {
            is CatalogScreenModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = onDismissRequest,
                    filters = state.filters,
                    isLoading = state.filterState is FilterUiState.Loading,
                    errorMessage = (state.filterState as? FilterUiState.Error)?.throwable?.message,
                    presets = if (feedsEnabled) screenModel.feedPresets() else emptyList(),
                    onReset = screenModel::resetFilters,
                    onApplyPreset = screenModel::applyPreset,
                    onEditPreset = screenModel::showEditPresetDialog,
                    onDeletePreset = { presetPendingDeletion = it },
                    canDeletePreset = screenModel::canDeletePreset,
                    onSaveAsNewPreset = if (feedsEnabled) screenModel::showSavePresetDialog else null,
                    currentPresetName = appliedCustomPreset?.name,
                    onUpdateCurrentPreset = if (feedsEnabled) screenModel::showUpdateCurrentPresetDialog else null,
                    onFilter = { screenModel.search(filters = state.filters) },
                    onUpdate = screenModel::setFilters,
                    onRetry = screenModel::retryFilterLoad,
                )
            }
            is CatalogScreenModel.Dialog.SavePreset -> if (feedsEnabled) {
                BrowseFeedNameDialog(
                    title = when (dialog.mode) {
                        CatalogScreenModel.Dialog.SavePreset.Mode.Create -> MR.strings.browse_feed_save_preset
                        CatalogScreenModel.Dialog.SavePreset.Mode.EditMetadata -> MR.strings.action_edit
                        CatalogScreenModel.Dialog.SavePreset.Mode.UpdateFromCurrentState ->
                            MR.strings.browse_feed_update_preset
                    },
                    initialValue = dialog.name,
                    initialChronological = dialog.chronological,
                    duplicateName = { name ->
                        screenModel.hasPresetName(name, excludingPresetId = dialog.presetId)
                    },
                    onDismissRequest = onDismissRequest,
                    onConfirm = screenModel::savePreset,
                )
            }
            is CatalogScreenModel.Dialog.EntryPreview -> {
                BrowseEntryPreviewSheet(
                    entryId = dialog.entryId,
                    onLibraryAction = screenModel::confirmBrowseLibraryAction,
                    onMergeAction = screenModel::showMergeTargetPicker,
                    onOpenEntry = {
                        onDismissRequest()
                        navigator.push(EntryScreen(it, fromSource = true))
                    },
                    onDismissRequest = onDismissRequest,
                )
            }
            is CatalogScreenModel.Dialog.LibraryActionChooser -> {
                BrowseLibraryActionDialog(
                    mangaTitle = dialog.entry.displayTitle,
                    favorite = dialog.entry.favorite,
                    onDismissRequest = onDismissRequest,
                    onLibraryAction = {
                        onDismissRequest()
                        screenModel.confirmBrowseLibraryAction(
                            CatalogListItem.EntryItem(dialog.entry, screenModel.sourceItemOrientation),
                        )
                    },
                    onMergeIntoLibrary = {
                        screenModel.showMergeTargetPicker(
                            CatalogListItem.EntryItem(dialog.entry, screenModel.sourceItemOrientation),
                        )
                    },
                )
            }
            CatalogScreenModel.Dialog.CheckingDuplicates -> DuplicateDetectionLoadingDialog()
            is CatalogScreenModel.Dialog.RemoveEntry -> {
                RemoveMangaDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        screenModel.changeFavorite(
                            CatalogListItem.EntryItem(dialog.entry, screenModel.sourceItemOrientation),
                        )
                    },
                    mangaToRemove = dialog.entry,
                )
            }
            is CatalogScreenModel.Dialog.DuplicateEntry -> {
                DuplicateEntryDialog(
                    duplicates = dialog.duplicates,
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        screenModel.addFavorite(
                            CatalogListItem.EntryItem(dialog.entry, screenModel.sourceItemOrientation),
                        )
                    },
                    onOpenEntry = { navigator.push(EntryScreen(it.id, fromSource = true)) },
                    onMigrate = {
                        screenModel.showMigrateEntryDialog(current = it, target = dialog.entry)
                    },
                )
            }
            is CatalogScreenModel.Dialog.SelectEntryMergeTarget -> {
                MergeTargetPickerDialog(
                    title = stringResource(MR.strings.action_merge_into_library),
                    query = dialog.query,
                    visibleTargets = dialog.visibleTargets,
                    onDismissRequest = onDismissRequest,
                    onQueryChange = screenModel::updateMergeTargetQuery,
                    onSelectTarget = screenModel::openMergeEditor,
                )
            }
            is CatalogScreenModel.Dialog.EditEntryMerge -> {
                BrowseMergeEditorDialog(
                    entries = dialog.entries,
                    targetId = dialog.targetId,
                    targetLocked = dialog.targetLocked,
                    removedIds = dialog.removedIds,
                    libraryRemovalIds = dialog.libraryRemovalIds,
                    confirmEnabled = dialog.enabled,
                    onDismissRequest = onDismissRequest,
                    onMove = screenModel::moveMergeEntry,
                    onSelectTarget = screenModel::setMergeTarget,
                    onToggleRemove = screenModel::toggleMergeEntryRemoval,
                    onToggleLibraryRemove = screenModel::toggleMergeEntryLibraryRemoval,
                    onConfirm = screenModel::confirmBrowseMerge,
                )
            }
            is CatalogScreenModel.Dialog.ChangeEntryCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        screenModel.addFavorite(dialog.entry, include)
                    },
                )
            }
            is CatalogScreenModel.Dialog.MigrateEntry -> {
                MigrateEntryDialog(
                    current = dialog.current,
                    target = dialog.target,
                    onClickTitle = { navigator.push(EntryScreen(dialog.current.id, fromSource = true)) },
                    onDismissRequest = onDismissRequest,
                    onComplete = {
                        screenModel.dismissDialog()
                        navigator.push(EntryScreen(dialog.target.id, fromSource = true))
                    },
                )
            }
            null -> {}
        }

        presetPendingDeletion
            ?.takeIf { feedsEnabled }
            ?.let { presetId -> screenModel.feedPresets().firstOrNull { it.id == presetId } }
            ?.let { preset ->
                DeleteBrowsePresetDialog(
                    presetName = preset.name,
                    onDismissRequest = { presetPendingDeletion = null },
                    onConfirm = { screenModel.removePreset(preset.id) },
                )
            }

        LaunchedEffect(Unit) {
            queryEvent.receiveAsFlow()
                .collectLatest {
                    when (it) {
                        is SearchType.Genre -> screenModel.searchGenre(it.txt)
                        is SearchType.Text -> screenModel.search(it.txt)
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

@Composable
private fun CatalogToolbar(
    title: String,
    searchQuery: String?,
    onSearchQueryChange: (String?) -> Unit,
    displayMode: LibraryDisplayMode,
    onDisplayModeChange: (LibraryDisplayMode) -> Unit,
    navigateUp: () -> Unit,
    onWebViewClick: (() -> Unit)?,
    onSettingsClick: (() -> Unit)?,
    onLongPressActionsClick: () -> Unit,
    onSearch: (String) -> Unit,
    onEnterImmersive: (() -> Unit)?,
) {
    var selectingDisplayMode by remember { mutableStateOf(false) }

    SearchToolbar(
        navigateUp = navigateUp,
        titleContent = { AppBarTitle(title) },
        searchQuery = searchQuery,
        onChangeSearchQuery = onSearchQueryChange,
        onSearch = onSearch,
        onClickCloseSearch = navigateUp,
        actions = {
            AppBarActions(
                actions = buildList {
                    onEnterImmersive?.let {
                        add(
                            AppBar.Action(
                                title = stringResource(MR.strings.browse_enter_immersive),
                                icon = Icons.Outlined.Fullscreen,
                                onClick = it,
                            ),
                        )
                    }
                    add(
                        AppBar.Action(
                            title = stringResource(MR.strings.action_display_mode),
                            icon = if (displayMode == LibraryDisplayMode.List ||
                                displayMode == LibraryDisplayMode.ComfortableList
                            ) {
                                Icons.AutoMirrored.Filled.ViewList
                            } else {
                                Icons.Filled.ViewModule
                            },
                            onClick = { selectingDisplayMode = true },
                        ),
                    )
                    onWebViewClick?.let {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_open_in_web_view),
                                onClick = it,
                            ),
                        )
                    }
                    onSettingsClick?.let {
                        add(
                            AppBar.OverflowAction(
                                title = stringResource(MR.strings.action_settings),
                                onClick = it,
                            ),
                        )
                    }
                    add(
                        AppBar.OverflowAction(
                            title = stringResource(MR.strings.pref_browse_long_press_action_open_settings),
                            onClick = onLongPressActionsClick,
                        ),
                    )
                },
            )

            DropdownMenu(
                expanded = selectingDisplayMode,
                onDismissRequest = { selectingDisplayMode = false },
            ) {
                RadioMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_display_comfortable_grid)) },
                    isChecked = displayMode == LibraryDisplayMode.ComfortableGrid,
                ) {
                    selectingDisplayMode = false
                    onDisplayModeChange(LibraryDisplayMode.ComfortableGrid)
                }
                RadioMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_display_comfortable_list)) },
                    isChecked = displayMode == LibraryDisplayMode.ComfortableList,
                ) {
                    selectingDisplayMode = false
                    onDisplayModeChange(LibraryDisplayMode.ComfortableList)
                }
                RadioMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_display_grid)) },
                    isChecked = displayMode == LibraryDisplayMode.CompactGrid,
                ) {
                    selectingDisplayMode = false
                    onDisplayModeChange(LibraryDisplayMode.CompactGrid)
                }
                RadioMenuItem(
                    text = { Text(text = stringResource(MR.strings.action_display_list)) },
                    isChecked = displayMode == LibraryDisplayMode.List,
                ) {
                    selectingDisplayMode = false
                    onDisplayModeChange(LibraryDisplayMode.List)
                }
            }
        },
    )
}
