package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.material3.Icon
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.browse.CatalogContent
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreenEventsEffect
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogViewModel
import eu.kanade.tachiyomi.ui.browse.catalog.FilterUiState
import eu.kanade.tachiyomi.ui.browse.extension.details.SourcePreferencesScreen
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.launch
import mihon.entry.interactions.migration.EntryMigrationFeature
import mihon.entry.interactions.migration.EntryMigrationPreparationResult
import mihon.entry.interactions.migration.EntryMigrationPrepareIntent
import mihon.entry.interactions.source.EntrySourceHomeFeature
import mihon.entry.interactions.source.EntrySourceHomeResolution
import mihon.feature.migration.dialog.MigrateEntryDialog
import mihon.feature.migration.list.MigrationListScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.FilterList
import mihon.presentation.core.util.collectAsLazyPagingItems
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

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
        val migration = remember { Injekt.get<EntryMigrationFeature>() }
        val viewModel = viewModel {
            CatalogViewModel(
                sourceId = sourceId,
                listingQuery = query,
                migrationEntryType = currentEntry.type,
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
                SmallExtendedFloatingActionButton(
                    text = { Text(text = stringResource(MR.strings.action_filter)) },
                    icon = { Icon(MaterialSymbols.Rounded.FilterList, contentDescription = null) },
                    onClick = viewModel::openFilterSheet,
                    modifier = Modifier.animateFloatingActionButton(
                        visible = state.filters.isNotEmpty() || viewModel.hasFilterCapability,
                        alignment = Alignment.BottomEnd,
                    ),
                )
            },
            snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
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
                columns = viewModel.getColumnsPreference(
                    LocalConfiguration.current.orientation,
                    viewModel.sourceItemOrientation,
                ),
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
                    val home = source?.let {
                        Injekt.get<EntrySourceHomeFeature>().resolve(it.id)
                    } as? EntrySourceHomeResolution.Available
                    if (source != null && home != null) {
                        navigator.push(
                            WebViewScreen(
                                url = home.url,
                                initialTitle = source.name,
                                sourceId = source.id,
                            ),
                        )
                    }
                },
                onSettingsClick = if (viewModel.isConfigurable) {
                    { navigator.push(SourcePreferencesScreen(sourceId)) }
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
                        scope.launch {
                            HomeScreen.openTab(HomeScreen.Tab.Browse())
                        }
                        navigator.push(EntryScreen(dialog.target.id))
                    },
                )
            }
            else -> {}
        }
    }
}
