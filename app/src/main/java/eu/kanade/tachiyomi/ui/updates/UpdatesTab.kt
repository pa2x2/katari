package eu.kanade.tachiyomi.ui.updates

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.translation.EntryTranslateSetupSheet
import eu.kanade.presentation.library.update.rememberLibraryUpdateStarter
import eu.kanade.presentation.updates.UpdatesBottomBarConfig
import eu.kanade.presentation.updates.UpdatesDeleteConfirmationDialog
import eu.kanade.presentation.updates.UpdatesFilterDialog
import eu.kanade.presentation.updates.UpdatesFilterSummaryBar
import eu.kanade.presentation.updates.UpdatesFilteredEmptyScreen
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
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.feature.upcoming.UpcomingScreen
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.updates.model.UpdateItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

data object UpdatesTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 1u,
                title = stringResource(MR.strings.label_recent_updates),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(DownloadQueueScreen)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        val entryOpenFeature = remember { Injekt.get<EntryOpenFeature>() }
        val screenModel = rememberScreenModel { UpdatesScreenModel() }
        val settingsScreenModel = rememberScreenModel { UpdatesSettingsScreenModel() }
        val state by screenModel.state.collectAsState()
        val translationStatuses by screenModel.translationStatuses.collectAsStateWithLifecycle()
        val translationSetup by screenModel.translation.sheet.collectAsStateWithLifecycle()

        val categories by settingsScreenModel.getCategories.subscribe().collectAsState(initial = emptyList())
        val feed = state.feed
        val filterSummary = updatesFeedFilterSummary(feed.filter, categories, feed.sources)

        val latestUpdateSummary by screenModel.latestUpdateSummary.collectAsStateWithLifecycle()
        val updateStarter = rememberLibraryUpdateStarter(
            snackbarHostState = screenModel.snackbarHostState,
            startSkippedOfLatest = screenModel::updateSkippedOfLatest,
        )

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
                        screenModel.bookmarkUpdates(state.selected, true)
                    }.takeIf {
                        screenModel.hasBookmarkAction(selected, bookmark = true)
                    },
                    onRemoveBookmarkClicked = {
                        screenModel.bookmarkUpdates(state.selected, false)
                    }.takeIf {
                        screenModel.hasBookmarkAction(selected, bookmark = false)
                    },
                    onMarkAsReadClicked = {
                        screenModel.markUpdatesConsumed(state.selected, true)
                    }.takeIf {
                        screenModel.hasConsumedAction(selected, consumed = true)
                    },
                    onMarkAsUnreadClicked = {
                        screenModel.markUpdatesConsumed(state.selected, false)
                    }.takeIf {
                        screenModel.hasConsumedAction(selected, consumed = false)
                    },
                    markAsReadLabel = actionLabels.markAsReadLabel,
                    markAsUnreadLabel = actionLabels.markAsUnreadLabel,
                    onDownloadClicked = {
                        screenModel.downloadChapters(state.selected, ChapterDownloadAction.START)
                    }.takeIf {
                        screenModel.canUseDownloadActions(selected) && selected.any {
                            it.update is UpdateItem.EntryUpdate &&
                                it.downloadAvailable &&
                                it.downloadStateProvider() != EntryDownloadState.DOWNLOADED
                        }
                    },
                    onDeleteClicked = {
                        screenModel.showConfirmDeleteChapters(state.selected)
                    }.takeIf {
                        screenModel.canUseDownloadActions(selected) && selected.any {
                            it.update is UpdateItem.EntryUpdate &&
                                it.downloadStateProvider() == EntryDownloadState.DOWNLOADED
                        }
                    },
                ),
            ),
            snackbarHostState = screenModel.snackbarHostState,
            onSelectAll = screenModel::toggleAllSelection,
            onInvertSelection = screenModel::invertSelection,
            onUpdateLibrary = { updateStarter.start(screenModel::updateLibrary) },
            onCalendarClicked = { navigator.push(UpcomingScreen()) },
            onFilterClicked = screenModel::showFilterDialog,
            hasActiveFilters = feed.filter.isActive,
            filterSummary = if (feed.filter.isActive && state.items.isNotEmpty()) {
                {
                    UpdatesFilterSummaryBar(
                        summary = filterSummary,
                        hiddenByFilters = feed.hiddenByFilters,
                        onClick = screenModel::showFilterDialog,
                        onClear = settingsScreenModel::clearFilters,
                    )
                }
            } else {
                null
            },
            emptyContent = { modifier ->
                if (feed.hiddenByFilters > 0) {
                    UpdatesFilteredEmptyScreen(
                        summary = filterSummary,
                        onClearFilters = settingsScreenModel::clearFilters,
                        modifier = modifier,
                    )
                } else {
                    EmptyScreen(stringRes = MR.strings.information_no_recent, modifier = modifier)
                }
            },
        ) {
            updatesLastUpdatedItem(
                lastUpdated = screenModel.lastUpdated,
                failed = latestUpdateSummary?.failed ?: 0,
                onClick = { navigator.push(LibraryUpdateReportScreen()) }.takeIf { latestUpdateSummary != null },
            )
            unifiedUpdatesUiItems(
                uiModels = state.getUiModel(),
                selectionMode = state.selectionMode,
                onUpdateSelected = screenModel::toggleSelection,
                onClickCover = { item ->
                    navigator.push(EntryScreen(item.visibleEntryId))
                },
                isOpenApplicable = { item -> entryOpenFeature.isApplicable(item.update.entryType) },
                onClickUpdate = { item ->
                    scope.launchIO {
                        val entry = Injekt.get<EntryRepository>().getEntryById(item.visibleEntryId)
                            ?: return@launchIO
                        val chapterId = when (val update = item.update) {
                            is UpdateItem.EntryUpdate -> update.update.chapterId
                        }
                        val chapter = Injekt.get<EntryChapterRepository>().getChapterById(chapterId)
                            ?: return@launchIO
                        entryOpenFeature.open(context, entry, chapter)
                    }
                },
                onDownloadChapter = screenModel::downloadChapters,
                translateStatusOf = { translationStatuses[(it.update as? UpdateItem.EntryUpdate)?.update?.chapterId] },
                isTranslateApplicable = { screenModel.translation.isApplicable(it.update.entryType) },
                onTranslateChapter = screenModel::translateChapter,
            )
            updatesFeedFooterItem(
                hiddenByFilters = feed.hiddenByFilters,
                fromHiddenSources = feed.fromHiddenSources,
                onClearFilters = settingsScreenModel::clearFilters,
            )
        }

        translationSetup?.let { EntryTranslateSetupSheet(it, screenModel.translation) }

        val onDismissDialog = { screenModel.setDialog(null) }
        when (val dialog = state.dialog) {
            is UpdatesScreenModel.Dialog.DeleteConfirmation -> {
                UpdatesDeleteConfirmationDialog(
                    entryTypes = dialog.toDelete.map { it.update.entryType },
                    onDismissRequest = onDismissDialog,
                    onConfirm = { screenModel.deleteChapters(dialog.toDelete) },
                )
            }
            is UpdatesScreenModel.Dialog.FilterSheet -> {
                UpdatesFilterDialog(
                    onDismissRequest = onDismissDialog,
                    screenModel = settingsScreenModel,
                    filter = feed.filter,
                    entryTypes = feed.entryTypes,
                    sources = feed.sources,
                )
            }
            null -> {}
        }

        LaunchedEffect(state.selectionMode) {
            HomeScreen.showBottomNav(!state.selectionMode)
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }
        DisposableEffect(Unit) {
            screenModel.resetNewUpdatesCount()

            onDispose {
                screenModel.resetNewUpdatesCount()
            }
        }
    }
}
