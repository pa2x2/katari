package eu.kanade.tachiyomi.ui.browse.migration.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.migration.entry.components.MigrateEntrySelectionGroup
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import mihon.feature.migration.config.MigrationConfigScreen
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowForward
import mihon.icons.materialsymbols.rounded.Deselect
import mihon.icons.materialsymbols.rounded.SelectAll
import mihon.presentation.kit.button.FloatingActionButton
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.AppBarTitle
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.navigation.SearchToolbar
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.shouldExpandFAB

data class MigrateEntriesScreen(
    private val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = viewModel { MigrateEntriesViewModel(sourceId) }

        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        BackHandler(enabled = state.searchQuery != null) {
            viewModel.setSearchQuery(null)
        }

        val lazyListState = rememberLazyListState()

        Scaffold(
            topBar = { scrollBehavior ->
                SearchToolbar(
                    searchQuery = state.searchQuery,
                    onChangeSearchQuery = viewModel::setSearchQuery,
                    titleContent = {
                        AppBarTitle(
                            title = stringResource(MR.strings.migrationEntriesScreen_title),
                            subtitle = buildString {
                                append(state.source!!.name)
                                append(" • ")
                                append(
                                    stringResource(
                                        MR.strings.migrationEntriesScreen_selectedCount,
                                        state.selectedCount,
                                        state.items.size,
                                    ),
                                )
                            },
                        )
                    },
                    navigateUp = navigator::pop,
                    actions = {
                        val hasSearchFilter = !state.searchQuery.isNullOrBlank()
                        val selectAllLabel = stringResource(
                            if (hasSearchFilter) {
                                MR.strings.migrationEntriesScreen_selectAllResults
                            } else {
                                MR.strings.action_select_all
                            },
                        )
                        val deselectAllLabel = stringResource(
                            if (hasSearchFilter) {
                                MR.strings.migrationEntriesScreen_deselectAllResults
                            } else {
                                MR.strings.migrationEntriesScreen_deselectAll
                            },
                        )
                        AppBarActions(
                            listOf(
                                AppBar.Action(
                                    title = selectAllLabel,
                                    icon = MaterialSymbols.Rounded.SelectAll,
                                    onClick = viewModel::selectAllVisible,
                                    enabled = state.visibleItems.isNotEmpty() && !state.allVisibleSelected,
                                ),
                                AppBar.Action(
                                    title = deselectAllLabel,
                                    icon = MaterialSymbols.Rounded.Deselect,
                                    onClick = viewModel::deselectAllVisible,
                                    enabled = state.visibleSelectionCount > 0,
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    label = stringResource(MR.strings.migrationConfigScreen_continueButtonText),
                    icon = MaterialSymbols.AutoMirroredRounded.ArrowForward,
                    onClick = {
                        val groups = viewModel.migrationGroups()
                        if (groups.isNotEmpty()) {
                            navigator.push(MigrationConfigScreen(groups, sourceId))
                        }
                    },
                    expanded = lazyListState.shouldExpandFAB(),
                    visible = state.selectionMode,
                )
            },
        ) { contentPadding ->
            when {
                state.isEmpty -> {
                    EmptyScreen(
                        stringRes = MR.strings.empty_screen,
                        modifier = Modifier.padding(contentPadding),
                    )
                }
                state.hasNoSearchResults -> {
                    EmptyScreen(
                        stringRes = MR.strings.no_results_found,
                        modifier = Modifier.padding(contentPadding),
                    )
                }
                else -> {
                    MigrateEntriesContent(
                        lazyListState = lazyListState,
                        contentPadding = contentPadding,
                        state = state,
                        onToggleSelection = viewModel::toggleSelection,
                        onToggleGroup = viewModel::toggleGroupSelection,
                        onLongClickItem = { entryId ->
                            if (state.selectionMode) {
                                viewModel.toggleRangeSelection(entryId)
                            } else {
                                viewModel.toggleSelection(entryId)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onInspect = { entryId, bypassMerge ->
                            navigator.push(EntryScreen(entryId, bypassMerge = bypassMerge))
                        },
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            viewModel.events.collectLatest { event ->
                when (event) {
                    MigrationEntriesEvent.FailedFetchingFavorites -> {
                        context.toast(MR.strings.internal_error)
                    }
                }
            }
        }
    }
}

@Composable
private fun MigrateEntriesContent(
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
    state: MigrateEntriesViewModel.State,
    onToggleSelection: (Long) -> Unit,
    onToggleGroup: (Set<Long>) -> Unit,
    onLongClickItem: (Long) -> Unit,
    onInspect: (Long, Boolean) -> Unit,
) {
    FastScrollLazyColumn(
        state = lazyListState,
        contentPadding = contentPadding + PaddingValues(vertical = KatariTheme.spacing.s),
    ) {
        items(
            items = state.visibleGroups,
            key = { it.key },
            contentType = { if (it.isMerged) "migration_entry_selection_group" else "migration_entry_selection_item" },
        ) { group ->
            MigrateEntrySelectionGroup(
                group = group,
                itemOrientation = state.itemOrientation,
                selection = state.selection,
                onToggleSelection = onToggleSelection,
                onToggleGroup = onToggleGroup,
                onLongClickItem = onLongClickItem,
                onInspect = onInspect,
                modifier = Modifier.padding(
                    horizontal = KatariTheme.spacing.s,
                    vertical = KatariTheme.spacing.xs,
                ),
            )
        }
    }
}
