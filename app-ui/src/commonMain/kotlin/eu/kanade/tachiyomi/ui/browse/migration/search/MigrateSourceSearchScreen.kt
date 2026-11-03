package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.browse.CatalogContent
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreenEventsEffect
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogViewModel
import eu.kanade.tachiyomi.ui.browse.catalog.FilterUiState
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import kotlinx.coroutines.launch
import mihon.app.ui.di.LocalAppUiGraph
import mihon.entry.interactions.migration.EntryMigrationPreparationResult
import mihon.entry.interactions.migration.EntryMigrationPrepareIntent
import mihon.feature.migration.dialog.MigrateEntryDialog
import mihon.feature.migration.list.MigrationListScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.presentation.core.util.collectAsLazyPagingItems
import mihon.presentation.kit.button.FloatingActionButton
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.navigation.SearchToolbar
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

data class MigrateSourceSearchScreen(
    private val currentEntry: Entry,
    private val sourceId: Long,
    private val query: String?,
) : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val graph = LocalAppUiGraph.current
        val migration = remember { graph.entryMigrationFeature }
        val sourcePreferencesDestination = remember { graph.sourcePreferencesDestination }
        val webViewDestination = remember { graph.webViewDestination }
        val viewModel = assistedMetroViewModel<CatalogViewModel, CatalogViewModel.Factory> {
            create(
                sourceId = sourceId,
                listingQuery = query,
                migrationEntryType = currentEntry.type,
                initialFilterSnapshot = emptyList(),
                initialPresetId = null,
                openFilterSheetOnStart = false,
                feedProfileId = null,
            )
        }
        val state by viewModel.state.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        val migrationFailureMessage = stringResource(MR.strings.internal_error)

        val catalogList = viewModel.catalogPagerFlowFlow.collectAsLazyPagingItems()
        CatalogScreenEventsEffect(viewModel, snackbarHostState)

        Scaffold(
            topBar = { scrollBehavior ->
                SearchToolbar(
                    searchQuery = state.toolbarQuery ?: "",
                    onChangeSearchQuery = viewModel::setToolbarQuery,
                    onClickCloseSearch = navigator::pop,
                    onSearch = viewModel::search,
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    label = stringResource(MR.strings.action_filter),
                    icon = MaterialSymbols.Rounded.FilterList,
                    onClick = viewModel::openFilterSheet,
                    visible = state.filters.isNotEmpty() || viewModel.hasFilterCapability,
                )
            },
            snackbarHostState = snackbarHostState,
        ) { paddingValues ->
            val openMigrateDialog: (Entry) -> Unit = openMigrateDialog@{ target ->
                if (target.type != currentEntry.type) return@openMigrateDialog
                scope.launch {
                    val preparation = migration.prepare(EntryMigrationPrepareIntent(currentEntry, target))
                    if (preparation !is EntryMigrationPreparationResult.Ready) {
                        snackbarHostState.showSnackbar(migrationFailureMessage)
                        return@launch
                    }
                    val migrateListScreen = navigator.items
                        .filterIsInstance<MigrationListScreen>()
                        .lastOrNull()

                    if (migrateListScreen == null) {
                        viewModel.showMigrateEntryDialog(current = currentEntry, target = target)
                    } else {
                        migrateListScreen.addMatchOverride(current = currentEntry.id, target = target.id)
                        navigator.popUntil { screen -> screen is MigrationListScreen }
                    }
                }
            }
            CatalogContent(
                catalogList = catalogList,
                coverSize = viewModel.coverSize,
                displayMode = viewModel.displayMode,
                sourceItemOrientation = viewModel.sourceItemOrientation,
                snackbarHostState = snackbarHostState,
                contentPadding = paddingValues,
                onItemClick = { item ->
                    if (item is CatalogListItem.EntryItem) {
                        openMigrateDialog(item.entry)
                    }
                },
                onItemLongClick = { item ->
                    if (item is CatalogListItem.EntryItem) {
                        navigator.push(EntryScreen(item.entry.id, fromSource = true))
                    }
                },
                onWebViewClick = {
                    val source = viewModel.catalogSource
                    val homeUrl = viewModel.homeUrl
                    if (source != null && homeUrl != null) {
                        navigator.push(
                            webViewDestination.screen(
                                url = homeUrl,
                                title = source.name,
                                sourceId = source.id,
                                headers = emptyMap(),
                            ),
                        )
                    }
                }.takeIf { webViewDestination.isAvailable },
                onSettingsClick = if (sourcePreferencesDestination.isAvailable(sourceId)) {
                    { navigator.push(sourcePreferencesDestination.screen(sourceId)) }
                } else {
                    null
                },
            )
        }

        val onDismissRequest = viewModel::dismissDialog
        when (val dialog = state.dialog) {
            is CatalogViewModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = onDismissRequest,
                    filters = state.filters,
                    defaultFilters = state.defaultFilters,
                    filterRevision = state.filterRevision,
                    isLoading = state.filterState is FilterUiState.Loading,
                    errorMessage = (state.filterState as? FilterUiState.Error)?.throwable?.message,
                    onReset = viewModel::resetFilters,
                    onResetGroup = viewModel::resetFilterGroup,
                    pendingFilterEdits = state.pendingFilterEdits,
                    draftQuery = state.draftSearchQuery,
                    onEditPagedItem = viewModel::editPagedFilterItem,
                    onFilter = viewModel::applyDraftFilters,
                    onUpdate = viewModel::setFilters,
                    onRequestSuggestions = viewModel::filterSuggestions,
                    onRequestPagedFilterItems = viewModel::pagedFilterItems,
                    onRequestPagedFilterNavigation = viewModel::pagedFilterNavigation,
                    pagedFilterBrowseSession = viewModel::pagedFilterBrowseSession,
                    onRetry = viewModel::retryFilterLoad,
                )
            }
            is CatalogViewModel.Dialog.MigrateEntry -> {
                MigrateEntryDialog(
                    current = dialog.current,
                    target = dialog.target,
                    onClickTitle = {
                        navigator.push(EntryScreen(dialog.target.id, fromSource = true))
                    },
                    onDismissRequest = onDismissRequest,
                    onComplete = {
                        navigator.popUntilRoot()
                        scope.launch { HomeScreen.openTab(HomeScreen.Tab.Browse()) }
                        navigator.push(EntryScreen(dialog.target.id))
                    },
                )
            }
            else -> {}
        }
    }
}
