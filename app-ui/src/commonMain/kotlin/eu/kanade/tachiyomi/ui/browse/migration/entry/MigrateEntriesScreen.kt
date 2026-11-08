package eu.kanade.tachiyomi.ui.browse.migration.entry

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.ArrowForward
import app.katari.icons.materialsymbols.rounded.Deselect
import app.katari.icons.materialsymbols.rounded.SelectAll
import app.katari.presentation.kit.button.FloatingActionButton
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.feedback.LoadingScreen
import app.katari.presentation.kit.feedback.rememberScreenMessages
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.list.RowGroupItem
import app.katari.presentation.kit.list.RowGroupPosition
import app.katari.presentation.kit.navigation.AppBar
import app.katari.presentation.kit.navigation.AppBarActions
import app.katari.presentation.kit.navigation.AppBarTitle
import app.katari.presentation.kit.navigation.Scaffold
import app.katari.presentation.kit.navigation.SearchToolbar
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.sourcemigration.config.MigrationConfigScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.migration.entry.components.MigrateEntrySelectionGroup
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import kotlinx.coroutines.flow.collectLatest
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import tachiyomi.presentation.core.util.shouldExpandFAB

data class MigrateEntriesScreen(
    private val sourceId: Long,
) : Screen() {

    @Composable
    override fun Content() {
        val messages = rememberScreenMessages()
        val haptic = LocalHapticFeedback.current
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = assistedMetroViewModel<MigrateEntriesViewModel, MigrateEntriesViewModel.Factory> {
            create(sourceId)
        }

        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = state.searchQuery != null,
            onBackCompleted = { viewModel.setSearchQuery(null) },
        )

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
                        messages.show(MR.strings.internal_error)
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
    RowList(
        fastScroll = true,
        state = lazyListState,
        contentPadding = contentPadding + PaddingValues(vertical = KatariTheme.spacing.s),
    ) {
        itemsIndexed(
            items = state.visibleGroups,
            key = { _, group -> group.key },
            contentType = { _, group ->
                if (group.isMerged) "migration_entry_selection_group" else "migration_entry_selection_item"
            },
        ) { index, group ->
            RowGroupItem(
                position = RowGroupPosition.of(index, state.visibleGroups.size),
                modifier = Modifier.animateItem(),
            ) {
                MigrateEntrySelectionGroup(
                    group = group,
                    itemOrientation = state.itemOrientation,
                    selection = state.selection,
                    onToggleSelection = onToggleSelection,
                    onToggleGroup = onToggleGroup,
                    onLongClickItem = onLongClickItem,
                    onInspect = onInspect,
                )
            }
        }
    }
}
