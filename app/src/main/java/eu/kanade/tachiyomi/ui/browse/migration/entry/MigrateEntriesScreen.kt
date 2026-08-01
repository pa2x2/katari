package eu.kanade.tachiyomi.ui.browse.migration.entry

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Deselect
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.components.SearchToolbar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.migration.entry.components.MigrateEntrySelectionItem
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.collectLatest
import mihon.feature.migration.config.MigrationConfigScreen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
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
        val screenModel = rememberScreenModel { MigrateEntriesScreenModel(sourceId) }

        val state by screenModel.state.collectAsState()

        if (state.isLoading) {
            LoadingScreen()
            return
        }

        BackHandler(enabled = state.searchQuery != null) {
            screenModel.setSearchQuery(null)
        }

        val lazyListState = rememberLazyListState()

        Scaffold(
            topBar = { scrollBehavior ->
                SearchToolbar(
                    searchQuery = state.searchQuery,
                    onChangeSearchQuery = screenModel::setSearchQuery,
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
                                    icon = Icons.Outlined.SelectAll,
                                    onClick = screenModel::selectAllVisible,
                                    enabled = state.visibleItems.isNotEmpty() && !state.allVisibleSelected,
                                ),
                                AppBar.Action(
                                    title = deselectAllLabel,
                                    icon = Icons.Outlined.Deselect,
                                    onClick = screenModel::deselectAllVisible,
                                    enabled = state.visibleSelectionCount > 0,
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            floatingActionButton = {
                SmallExtendedFloatingActionButton(
                    text = { Text(text = stringResource(MR.strings.migrationConfigScreen_continueButtonText)) },
                    icon = {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                    },
                    onClick = {
                        val selection = screenModel.migrationSelection()
                        if (selection.isNotEmpty()) {
                            navigator.push(MigrationConfigScreen(selection))
                        }
                    },
                    expanded = lazyListState.shouldExpandFAB(),
                    modifier = Modifier.animateFloatingActionButton(
                        visible = state.selectionMode,
                        alignment = Alignment.BottomEnd,
                    ),
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
                        onToggleSelection = screenModel::toggleSelection,
                        onLongClickItem = { entryId ->
                            if (state.selectionMode) {
                                screenModel.toggleRangeSelection(entryId)
                            } else {
                                screenModel.toggleSelection(entryId)
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onInspect = { navigator.push(EntryScreen(it)) },
                    )
                }
            }
        }

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
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
    state: MigrateEntriesScreenModel.State,
    onToggleSelection: (Long) -> Unit,
    onLongClickItem: (Long) -> Unit,
    onInspect: (Long) -> Unit,
) {
    FastScrollLazyColumn(
        state = lazyListState,
        contentPadding = contentPadding + PaddingValues(vertical = MaterialTheme.padding.small),
    ) {
        items(
            items = state.visibleItems,
            key = { it.entry.id },
            contentType = { "migration_entry_selection_item" },
        ) { item ->
            MigrateEntrySelectionItem(
                entry = item.entry,
                itemOrientation = state.itemOrientation,
                consumedCount = item.progress.consumedCount,
                totalCount = item.progress.totalCount,
                isSelected = item.entry.id in state.selection,
                onToggleSelection = { onToggleSelection(item.entry.id) },
                onLongClick = { onLongClickItem(item.entry.id) },
                onInspect = { onInspect(item.entry.id) },
            )
        }
    }
}
