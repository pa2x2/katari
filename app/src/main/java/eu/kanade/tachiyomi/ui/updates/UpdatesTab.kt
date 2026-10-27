package eu.kanade.tachiyomi.ui.updates

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.translation.EntryTranslateSetupSheet
import eu.kanade.presentation.library.update.rememberLibraryUpdateStarter
import eu.kanade.presentation.updates.UpdatesBottomBarConfig
import eu.kanade.presentation.updates.UpdatesDeleteConfirmationDialog
import eu.kanade.presentation.updates.UpdatesFilterDialog
import eu.kanade.presentation.updates.UpdatesFilterSummaryBar
import eu.kanade.presentation.updates.UpdatesFilteredEmptyScreen
import eu.kanade.presentation.updates.UpdatesLastUpdatedHeader
import eu.kanade.presentation.updates.UpdatesScreen
import eu.kanade.presentation.updates.UpdatesScreenState
import eu.kanade.presentation.updates.unifiedUpdatesUiItems
import eu.kanade.presentation.updates.updatesFeedFilterSummary
import eu.kanade.presentation.updates.updatesFeedFooterItem
import eu.kanade.presentation.updates.updatesLastUpdatedItem
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.entry.entrySelectionActionLabels
import eu.kanade.tachiyomi.ui.home.navigation.LocalHomeNavigation
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import mihon.entry.interactions.download.EntryDownloadState
import mihon.feature.upcoming.UpcomingScreen
import mihon.presentation.kit.feedback.EmptyScreen
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.updates.model.UpdateItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

data object UpdatesTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 1u,
            title = stringResource(MR.strings.label_recent_updates),
        )

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(DownloadQueueScreen)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val viewModel = metroViewModel<UpdatesViewModel>()
        val settingsViewModel = metroViewModel<UpdatesSettingsViewModel>()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val translationStatuses by viewModel.translationStatuses.collectAsStateWithLifecycle()
        val translationSetup by viewModel.translation.sheet.collectAsStateWithLifecycle()

        val categories by settingsViewModel.getCategories.subscribe().collectAsState(initial = emptyList())
        val feed = state.feed
        val filterSummary = updatesFeedFilterSummary(feed.filter, categories, feed.sources)

        val latestUpdateSummary by viewModel.latestUpdateSummary.collectAsStateWithLifecycle()
        val pausedSourceNames by viewModel.pausedSourceNames.collectAsStateWithLifecycle()
        // The last-updated header already shows how the latest update went and opens its report, so no result snackbar.
        val updateStarter = rememberLibraryUpdateStarter(viewModel.snackbarHostState)

        val openUpdateReport = { navigator.push(LibraryUpdateReportScreen()) }.takeIf { latestUpdateSummary != null }
        val selected = state.selected
        val actionLabels = selected.map { it.update.entryType }.entrySelectionActionLabels()

        UpdatesScreen(
            state = UpdatesScreenState<UpdatesItem>(
                isLoading = state.isLoading,
                isEmpty = state.items.isEmpty(),
                selectionMode = state.selectionMode,
                selectedCount = state.selected.size,
                bottomBarConfig = UpdatesBottomBarConfig(
                    visible = state.selected.isNotEmpty(),
                    onBookmarkClicked = {
                        viewModel.bookmarkUpdates(state.selected, true)
                    }.takeIf {
                        viewModel.hasBookmarkAction(selected, bookmark = true)
                    },
                    onRemoveBookmarkClicked = {
                        viewModel.bookmarkUpdates(state.selected, false)
                    }.takeIf {
                        viewModel.hasBookmarkAction(selected, bookmark = false)
                    },
                    onMarkAsReadClicked = {
                        viewModel.markUpdatesConsumed(state.selected, true)
                    }.takeIf {
                        viewModel.hasConsumedAction(selected, consumed = true)
                    },
                    onMarkAsUnreadClicked = {
                        viewModel.markUpdatesConsumed(state.selected, false)
                    }.takeIf {
                        viewModel.hasConsumedAction(selected, consumed = false)
                    },
                    markAsReadLabel = actionLabels.markAsReadLabel,
                    markAsUnreadLabel = actionLabels.markAsUnreadLabel,
                    onDownloadClicked = {
                        viewModel.downloadChapters(state.selected, ChapterDownloadAction.START)
                    }.takeIf {
                        viewModel.canUseDownloadActions(selected) && selected.any {
                            it.update is UpdateItem.EntryUpdate &&
                                it.downloadAvailable &&
                                it.downloadStateProvider() != EntryDownloadState.DOWNLOADED
                        }
                    },
                    onDeleteClicked = {
                        viewModel.showConfirmDeleteChapters(state.selected)
                    }.takeIf {
                        viewModel.canUseDownloadActions(selected) && selected.any {
                            it.update is UpdateItem.EntryUpdate &&
                                it.downloadStateProvider() == EntryDownloadState.DOWNLOADED
                        }
                    },
                ),
            ),
            snackbarHostState = viewModel.snackbarHostState,
            onSelectAll = viewModel::toggleAllSelection,
            onInvertSelection = viewModel::invertSelection,
            onUpdateLibrary = { updateStarter.start(viewModel::updateLibrary) },
            onCalendarClicked = { navigator.push(UpcomingScreen()) },
            onFilterClicked = viewModel::showFilterDialog,
            hasActiveFilters = feed.filter.isActive,
            filterSummary = if (feed.filter.isActive && state.items.isNotEmpty()) {
                {
                    UpdatesFilterSummaryBar(
                        summary = filterSummary,
                        hiddenByFilters = feed.hiddenByFilters,
                        onClick = viewModel::showFilterDialog,
                        onClear = settingsViewModel::clearFilters,
                    )
                }
            } else {
                null
            },
            emptyContent = { modifier ->
                // An update that found nothing still says when it ran and what it couldn't check
                Column(modifier = modifier.fillMaxSize()) {
                    UpdatesLastUpdatedHeader(
                        lastUpdated = viewModel.lastUpdated,
                        failed = latestUpdateSummary?.failed ?: 0,
                        pausedSources = pausedSourceNames,
                        onClick = openUpdateReport,
                    )
                    if (feed.hiddenByFilters > 0) {
                        UpdatesFilteredEmptyScreen(
                            summary = filterSummary,
                            onClearFilters = settingsViewModel::clearFilters,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        EmptyScreen(stringRes = MR.strings.information_no_recent, modifier = Modifier.weight(1f))
                    }
                }
            },
        ) {
            updatesLastUpdatedItem(
                lastUpdated = viewModel.lastUpdated,
                failed = latestUpdateSummary?.failed ?: 0,
                pausedSources = pausedSourceNames,
                onClick = openUpdateReport,
            )
            unifiedUpdatesUiItems(
                uiModels = state.getUiModel(),
                selectionMode = state.selectionMode,
                onUpdateSelected = viewModel::toggleSelection,
                onGroupSelected = viewModel::setGroupSelection,
                onToggleGroupExpanded = viewModel::toggleGroupExpanded,
                onClickCover = { item ->
                    navigator.push(EntryScreen(item.visibleEntryId))
                },
                isOpenApplicable = viewModel::isOpenApplicable,
                onClickUpdate = { item -> scope.launchIO { viewModel.openUpdate(context, item) } },
                onDownloadChapter = viewModel::downloadChapters,
                translateStatusOf = { translationStatuses[(it.update as? UpdateItem.EntryUpdate)?.update?.chapterId] },
                isTranslateApplicable = { viewModel.translation.isApplicable(it.update.entryType) },
                onTranslateChapter = viewModel::translateChapter,
            )
            updatesFeedFooterItem(
                hiddenByFilters = feed.hiddenByFilters,
                fromHiddenSources = feed.fromHiddenSources,
                onClearFilters = settingsViewModel::clearFilters,
            )
        }

        translationSetup?.let { EntryTranslateSetupSheet(it, viewModel.translation) }

        val onDismissDialog = { viewModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is UpdatesViewModel.Dialog.DeleteConfirmation -> {
                UpdatesDeleteConfirmationDialog(
                    entryTypes = dialog.toDelete.map { it.update.entryType },
                    onDismissRequest = onDismissDialog,
                    onConfirm = { viewModel.deleteChapters(dialog.toDelete) },
                )
            }
            is UpdatesViewModel.Dialog.FilterSheet -> {
                UpdatesFilterDialog(
                    onDismissRequest = onDismissDialog,
                    viewModel = settingsViewModel,
                    filter = feed.filter,
                    entryTypes = feed.entryTypes,
                    sources = feed.sources,
                )
            }
            null -> {}
        }

        val homeNavigation = LocalHomeNavigation.current
        LaunchedEffect(state.selectionMode) {
            homeNavigation.hiddenByTab = state.selectionMode
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }
        DisposableEffect(Unit) {
            viewModel.resetNewUpdatesCount()

            onDispose {
                viewModel.resetNewUpdatesCount()
            }
        }
    }
}
