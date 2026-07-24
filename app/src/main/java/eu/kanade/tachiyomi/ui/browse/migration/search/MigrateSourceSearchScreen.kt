package eu.kanade.tachiyomi.ui.browse.migration.search

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.paging.compose.LazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.browse.CatalogContent
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreenModel
import eu.kanade.tachiyomi.ui.browse.catalog.FilterUiState
import eu.kanade.tachiyomi.ui.browse.source.browse.SourceFilterDialog
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryMigrationFeature
import mihon.entry.interactions.EntryMigrationPreparationResult
import mihon.entry.interactions.EntryMigrationPrepareIntent
import mihon.entry.interactions.EntrySourceHomeFeature
import mihon.entry.interactions.EntrySourceHomeResolution
import mihon.feature.migration.dialog.MigrateEntryDialog
import mihon.feature.migration.list.MigrationListScreen
import mihon.presentation.core.util.collectAsLazyPagingItems
import tachiyomi.core.common.Constants
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.CatalogListItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.source.local.LocalSource
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

        val uriHandler = LocalUriHandler.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val migration = remember { Injekt.get<EntryMigrationFeature>() }
        val screenModel = rememberScreenModel {
            CatalogScreenModel(
                sourceId = sourceId,
                listingQuery = query,
                migrationEntryType = currentEntry.type,
            )
        }
        val state by screenModel.state.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        val migrationFailureMessage = stringResource(MR.strings.internal_error)

        @Suppress("UNCHECKED_CAST")
        val catalogList = screenModel.catalogPagerFlowFlow.collectAsLazyPagingItems() as
            LazyPagingItems<StateFlow<CatalogListItem>>

        Scaffold(
            topBar = { scrollBehavior ->
                SearchToolbar(
                    searchQuery = state.toolbarQuery ?: "",
                    onChangeSearchQuery = screenModel::setToolbarQuery,
                    onClickCloseSearch = navigator::pop,
                    onSearch = screenModel::search,
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                SmallExtendedFloatingActionButton(
                    text = { Text(text = stringResource(MR.strings.action_filter)) },
                    icon = { Icon(Icons.Outlined.FilterList, contentDescription = null) },
                    onClick = screenModel::openFilterSheet,
                    modifier = Modifier.animateFloatingActionButton(
                        visible = state.filters.isNotEmpty() || screenModel.hasFilterCapability,
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
                        screenModel.showMigrateEntryDialog(current = currentEntry, target = target)
                    } else {
                        migrateListScreen.addMatchOverride(current = currentEntry.id, target = target.id)
                        navigator.popUntil { screen -> screen is MigrationListScreen }
                    }
                }
            }
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
                        val source = screenModel.catalogSource
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
                    onSettingsClick = { uriHandler.openUri(Constants.URL_HELP) },
                )
            }
        }

        val onDismissRequest = screenModel::dismissDialog
        when (val dialog = state.dialog) {
            is CatalogScreenModel.Dialog.Filter -> {
                SourceFilterDialog(
                    onDismissRequest = onDismissRequest,
                    filters = state.filters,
                    isLoading = state.filterState is FilterUiState.Loading,
                    errorMessage = (state.filterState as? FilterUiState.Error)?.throwable?.message,
                    presets = emptyList(),
                    onReset = screenModel::resetFilters,
                    onApplyPreset = {},
                    onEditPreset = {},
                    onDeletePreset = {},
                    canDeletePreset = { false },
                    onSaveAsNewPreset = null,
                    onFilter = { screenModel.search(filters = state.filters) },
                    onUpdate = screenModel::setFilters,
                    onRetry = screenModel::retryFilterLoad,
                )
            }
            is CatalogScreenModel.Dialog.MigrateEntry -> {
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
