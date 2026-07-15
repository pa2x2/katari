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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.updates.UpdatesBottomBarConfig
import eu.kanade.presentation.updates.UpdatesDeleteConfirmationDialog
import eu.kanade.presentation.updates.UpdatesFilterDialog
import eu.kanade.presentation.updates.UpdatesScreen
import eu.kanade.presentation.updates.UpdatesScreenState
import eu.kanade.presentation.updates.unifiedUpdatesFilterOptions
import eu.kanade.presentation.updates.unifiedUpdatesUiItems
import eu.kanade.presentation.updates.updatesLastUpdatedItem
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.entry.entrySelectionActionLabels
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel.Event
import kotlinx.coroutines.flow.collectLatest
import mihon.entry.interactions.EntryDownloadState
import mihon.entry.interactions.EntryOpenInteraction
import mihon.feature.upcoming.UpcomingScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.updates.model.UpdateItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
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
        val entryOpenInteraction = remember { Injekt.get<EntryOpenInteraction>() }
        val screenModel = rememberScreenModel { UpdatesScreenModel() }
        val settingsScreenModel = rememberScreenModel { UpdatesSettingsScreenModel() }
        val state by screenModel.state.collectAsState()

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
                        selected.any {
                            it.update is UpdateItem.EntryUpdate &&
                                it.downloadSupported &&
                                it.downloadStateProvider() != EntryDownloadState.DOWNLOADED
                        }
                    },
                    onDeleteClicked = {
                        screenModel.showConfirmDeleteChapters(state.selected)
                    }.takeIf {
                        selected.any {
                            it.update is UpdateItem.EntryUpdate &&
                                it.downloadStateProvider() == EntryDownloadState.DOWNLOADED
                        }
                    },
                ),
            ),
            snackbarHostState = screenModel.snackbarHostState,
            onSelectAll = screenModel::toggleAllSelection,
            onInvertSelection = screenModel::invertSelection,
            onUpdateLibrary = screenModel::updateLibrary,
            onCalendarClicked = { navigator.push(UpcomingScreen()) },
            onFilterClicked = screenModel::showFilterDialog,
            hasActiveFilters = state.hasActiveFilters,
        ) {
            updatesLastUpdatedItem(screenModel.lastUpdated)
            unifiedUpdatesUiItems(
                uiModels = state.getUiModel(),
                selectionMode = state.selectionMode,
                onUpdateSelected = screenModel::toggleSelection,
                onClickCover = { item ->
                    navigator.push(EntryScreen(item.visibleEntryId))
                },
                onClickUpdate = { item ->
                    scope.launchIO {
                        val entry = Injekt.get<EntryRepository>().getEntryById(item.visibleEntryId)
                            ?: return@launchIO
                        val chapterId = when (val update = item.update) {
                            is UpdateItem.EntryUpdate -> update.update.chapterId
                        }
                        val chapter = Injekt.get<EntryChapterRepository>().getChapterById(chapterId)
                            ?: return@launchIO
                        entryOpenInteraction.open(context, entry, chapter)
                    }
                },
                onDownloadChapter = screenModel::downloadChapters,
            )
        }

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
                    options = unifiedUpdatesFilterOptions(),
                )
            }
            null -> {}
        }

        LaunchedEffect(Unit) {
            screenModel.events.collectLatest { event ->
                when (event) {
                    Event.InternalError -> screenModel.snackbarHostState.showSnackbar(
                        context.stringResource(MR.strings.internal_error),
                    )
                    is Event.LibraryUpdateTriggered -> {
                        val msg = if (event.started) {
                            MR.strings.updating_library
                        } else {
                            MR.strings.update_already_running
                        }
                        screenModel.snackbarHostState.showSnackbar(context.stringResource(msg))
                    }
                }
            }
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
