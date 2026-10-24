package eu.kanade.tachiyomi.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.components.MergeEditorSheet
import eu.kanade.presentation.entry.selectionEntryTypePresentation
import eu.kanade.presentation.library.DeleteLibraryEntriesDialog
import eu.kanade.presentation.library.EmptyLibraryScreen
import eu.kanade.presentation.library.MoveEntriesCategoryDialog
import eu.kanade.presentation.library.MoveEntriesConflictDialog
import eu.kanade.presentation.library.MoveEntriesProfileDialog
import eu.kanade.presentation.library.components.LibraryBottomActionMenu
import eu.kanade.presentation.library.components.LibraryContent
import eu.kanade.presentation.library.components.LibraryScrollToTopTarget
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.library.components.LibraryUpdateModeDialog
import eu.kanade.presentation.library.settings.LibrarySettingsDialog
import eu.kanade.presentation.library.update.rememberReportingLibraryUpdateStarter
import eu.kanade.presentation.more.onboarding.GETTING_STARTED_URL
import eu.kanade.presentation.more.settings.screen.data.rememberRestoreBackupLauncher
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateQueue
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.entry.entrySelectionActionLabels
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.security.BiometricAuthentication.authenticate
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.app.di.appGraph
import mihon.feature.migration.config.MigrationConfigScreen
import mihon.presentation.kit.feedback.LoadingScreen
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.navigation.rememberAppBarScrollBehavior
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.runtime.collectAsState as collectFlowAsState

data object LibraryTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = stringResource(MR.strings.label_library),
        )

    override suspend fun onReselect(navigator: Navigator) {
        reselectEvent.send(Unit)
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val profileManager = remember { context.appGraph.profileManager }
        val activeProfile by profileManager.activeProfile.collectFlowAsState()
        val visibleProfiles by profileManager.visibleProfiles.collectFlowAsState()

        val viewModel = metroViewModel<LibraryViewModel>()
        val settingsViewModel = metroViewModel<LibrarySettingsViewModel>(key = activeProfile?.id?.toString())
        val state by viewModel.state.collectAsStateWithLifecycle()
        val updateState by viewModel.updateState.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }
        val scrollToTopTarget = remember { LibraryScrollToTopTarget() }

        // A started update also reports its progress on the library's progress strip.
        val updateStarter = rememberReportingLibraryUpdateStarter(snackbarHostState) {
            LibraryUpdateQueue.startSkippedOfLatest(context)
        }

        val onClickRefresh: suspend (LibraryViewModel.State) -> Boolean = { state ->
            val activePage = state.activePage
            updateStarter.start {
                LibraryUpdateQueue.startNow(
                    context = context,
                    category = activePage?.category,
                    sourceId = activePage?.sourceId,
                    entryType = activePage?.entryType,
                )
            }
        }

        val onClickGlobalUpdate: suspend () -> Boolean = {
            updateStarter.start { LibraryUpdateQueue.startNow(context) }
        }

        val topBarScrollBehavior = rememberAppBarScrollBehavior()
        // Pages keep their own scroll positions but share the bar, so a newly selected page shows the title again, as its
        // group chips unfold.
        LaunchedEffect(state.coercedActivePageIndex) { topBarScrollBehavior.state.heightOffset = 0f }

        Scaffold(
            topBarScrollBehavior = topBarScrollBehavior,
            topBar = { scrollBehavior ->
                val title = state.getToolbarTitle(
                    defaultTitle = stringResource(MR.strings.label_library),
                    defaultCategoryTitle = stringResource(MR.strings.label_default),
                    page = state.coercedActivePageIndex,
                )
                LibraryToolbar(
                    hasActiveFilters = state.hasActiveFilters,
                    selectedCount = state.selection.size,
                    title = title,
                    currentGrouping = state.grouping,
                    onClickUnselectAll = viewModel::clearSelection,
                    onClickSelectAll = viewModel::selectAll,
                    onClickInvertSelection = viewModel::invertSelection,
                    onClickFilter = viewModel::showSettingsDialog,
                    onClickRefresh = { scope.launch { onClickRefresh(state) } },
                    onClickGlobalUpdate = { scope.launch { onClickGlobalUpdate() } },
                    onClickOpenRandomEntry = {
                        scope.launch {
                            val randomItem = viewModel.getRandomLibraryItemForCurrentPage()
                            if (randomItem != null) {
                                navigator.push(EntryScreen(randomItem.entry.id))
                            } else {
                                snackbarHostState.showSnackbar(
                                    context.stringResource(MR.strings.information_no_entries_found),
                                )
                            }
                        }
                    },
                    searchQuery = state.searchQuery,
                    onSearchQueryChange = viewModel::search,
                    scrollBehavior = scrollBehavior,
                )
            },
            bottomBar = {
                val actionLabels = state.selectedEntryTypes.entrySelectionActionLabels()
                val pinAction = state.selectionPinAction
                LibraryBottomActionMenu(
                    visible = state.selectionMode,
                    onPinClicked = viewModel::setSelectionPinned
                        .takeUnless { pinAction == LibraryPinSelectionAction.Hidden },
                    pinSelection = pinAction == LibraryPinSelectionAction.Pin,
                    onMergeClicked = viewModel::openMergeDialog.takeIf { viewModel.isMergeSelectionAvailable() },
                    onChangeCategoryClicked = viewModel::openChangeCategoryDialog,
                    onMarkAsReadClicked = { viewModel.markReadSelection(true) }
                        .takeIf { viewModel.canSetConsumedSelection() },
                    onMarkAsUnreadClicked = { viewModel.markReadSelection(false) }
                        .takeIf { viewModel.canSetConsumedSelection() },
                    markAsReadLabel = actionLabels.markAsReadLabel,
                    markAsUnreadLabel = actionLabels.markAsUnreadLabel,
                    downloadPresentation = state.selectedEntryTypes.selectionEntryTypePresentation(),
                    bookmarkedDownloadsSupported = viewModel.canDownloadSelection(
                        DownloadAction.BOOKMARKED_CHAPTERS,
                    ),
                    onDownloadClicked = viewModel::performDownloadAction
                        .takeIf { viewModel.canDownloadSelection() },
                    onDeleteClicked = viewModel::openDeleteEntriesDialog,
                    onMigrateClicked = {
                        val selection = viewModel.selectedMigrationSubjects()
                        viewModel.clearSelection()
                        navigator.push(MigrationConfigScreen(selection))
                    }.takeIf { viewModel.canMigrateSelection() },
                    onMoveToProfileClicked = viewModel::openMoveProfileDialog
                        .takeIf { visibleProfiles.any { it.id != activeProfile?.id } },
                    onCheckForUpdatesClicked = {
                        val entryIds = viewModel.takeSelectionForUpdate()
                        scope.launch { updateStarter.start { LibraryUpdateQueue.startSelection(context, entryIds) } }
                    },
                    onUpdateModeClicked = viewModel::openUpdateModeDialog,
                )
            },
            snackbarHostState = snackbarHostState,
        ) { contentPadding ->
            when {
                state.isLoading -> {
                    LoadingScreen(Modifier.padding(contentPadding))
                }
                state.searchQuery.isNullOrEmpty() && !state.hasActiveFilters && state.isLibraryEmpty -> {
                    val handler = LocalUriHandler.current
                    val restoreBackup = rememberRestoreBackupLauncher()
                    EmptyLibraryScreen(
                        onBrowseSources = {
                            scope.launch { HomeScreen.openTab(HomeScreen.Tab.Browse(BrowseTab.Page.Sources)) }
                        },
                        onRestoreBackup = restoreBackup,
                        onOpenGuide = { handler.openUri(GETTING_STARTED_URL) },
                        modifier = Modifier.padding(contentPadding),
                    )
                }
                else -> {
                    LibraryContent(
                        pages = state.displayedPages,
                        // Pages are built for the applied query, which trails typing; render what they match.
                        searchQuery = state.displayedPagesSearchQuery,
                        showSearchTips = state.searchQuery == "",
                        selection = state.selection,
                        contentPadding = contentPadding,
                        currentPage = state.coercedActivePageIndex,
                        hasActiveFilters = state.hasActiveFilters,
                        showPageTabs = state.showCategoryTabs,
                        showItemCounts = state.showEntryCount,
                        onChangeCurrentPage = { index ->
                            state.libraryData.profileId?.let { profileId ->
                                viewModel.onPageSettled(profileId, index)
                            }
                        },
                        onClickItem = { item ->
                            navigator.push(EntryScreen(item.entry.id))
                        },
                        onContinueReadingClicked = { item: LibraryItem ->
                            scope.launchIO {
                                viewModel.continueEntry(context, item.entry)
                            }
                            Unit
                        }.takeIf { state.showContinueButton },
                        isContinueReadingAvailable = { item ->
                            viewModel.canContinue(item.entry.type)
                        },
                        onToggleSelection = viewModel::toggleSelection,
                        onToggleRangeSelection = { page, item ->
                            viewModel.toggleRangeSelection(page, item)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onRefresh = { onClickRefresh(state) },
                        updateState = updateState,
                        onCancelUpdate = viewModel::cancelUpdate,
                        onGlobalSearchClicked = {
                            navigator.push(GlobalSearchScreen(viewModel.state.value.searchQuery ?: ""))
                        },
                        getDisplayMode = { viewModel.getDisplayMode() },
                        getColumnsForOrientation = { viewModel.getColumnsForOrientation(it) },
                        getItemsForPage = { state.getItemsForPage(it) },
                        displaySettingsForPage = state::displaySettingsForPage,
                        scrollToTopTarget = scrollToTopTarget,
                        onSearchQueryChange = viewModel::search,
                        onSeeAllSearchResults = { page ->
                            // Leave search on the chosen group's page; the model maps it onto the unsearched pages.
                            state.libraryData.profileId?.let { profileId -> viewModel.choosePage(profileId, page.id) }
                            viewModel.search(null)
                        },
                    )
                }
            }
        }

        val onDismissRequest = viewModel::closeDialog
        when (val dialog = state.dialog) {
            is LibraryViewModel.Dialog.SettingsSheet -> run {
                LibrarySettingsDialog(
                    onDismissRequest = onDismissRequest,
                    viewModel = settingsViewModel,
                    category = state.activeSortCategory,
                    filterAvailability = state.libraryData.filterAvailability,
                )
            }
            is LibraryViewModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = {
                        viewModel.clearSelection()
                        navigator.push(CategoryScreen())
                    },
                    onConfirm = { include, exclude ->
                        viewModel.clearSelection()
                        viewModel.setEntryCategories(dialog.items, include, exclude)
                    },
                )
            }
            is LibraryViewModel.Dialog.DeleteEntries -> {
                DeleteLibraryEntriesDialog(
                    containsLocalEntries = dialog.containsLocalEntries,
                    containsMergedEntries = dialog.containsMergedEntries,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { deleteFromLibrary, deleteChapter ->
                        viewModel.removeEntries(dialog.entries, deleteFromLibrary, deleteChapter)
                        viewModel.clearSelection()
                    },
                )
            }
            is LibraryViewModel.Dialog.MergeEntry -> {
                MergeLibraryEntriesSheet(
                    dialog = dialog,
                    onDismissRequest = onDismissRequest,
                    onMove = viewModel::reorderMergeSelection,
                    onSelectTarget = viewModel::setMergeTarget,
                    onConfirm = viewModel::confirmMergeSelection,
                )
            }
            is LibraryViewModel.Dialog.UpdateMode -> {
                LibraryUpdateModeDialog(
                    entryCount = dialog.entryIds.size,
                    currentMode = dialog.currentMode,
                    onDismissRequest = onDismissRequest,
                    onModeSelected = { mode -> viewModel.setUpdateMode(dialog.entryIds, mode) },
                )
            }
            is LibraryViewModel.Dialog.MoveProfile -> {
                MoveEntriesProfileDialog(
                    profiles = dialog.profiles,
                    onDismissRequest = onDismissRequest,
                    onProfileSelected = { profile ->
                        scope.launch {
                            val authenticated = if (profileManager.profileRequiresAuthNow(profile.id)) {
                                (context as? FragmentActivity)?.authenticate(
                                    title = context.stringResource(MR.strings.move_entries_auth_title),
                                    subtitle = context.stringResource(
                                        MR.strings.move_entries_auth_subtitle,
                                        profile.name,
                                    ),
                                ) == true
                            } else {
                                true
                            }
                            if (authenticated) {
                                viewModel.openMoveCategoryDialog(profile)
                            }
                        }
                    },
                )
            }
            is LibraryViewModel.Dialog.MoveCategory -> {
                MoveEntriesCategoryDialog(
                    categories = dialog.categories,
                    onDismissRequest = onDismissRequest,
                    onCategorySelected = { categoryId ->
                        viewModel.prepareMoveToProfile(dialog.profile, categoryId)
                    },
                )
            }
            is LibraryViewModel.Dialog.MoveConflict -> {
                val conflict = dialog.preview.conflicts[dialog.conflictIndex]
                MoveEntriesConflictDialog(
                    conflict = conflict,
                    conflictNumber = dialog.conflictIndex + 1,
                    conflictCount = dialog.preview.conflicts.size,
                    destinationProfileName = dialog.profile.name,
                    sourceName = viewModel.getSourceDisplayName(conflict.sourceEntry.source),
                    onDismissRequest = onDismissRequest,
                    onResolve = viewModel::resolveMoveConflict,
                )
            }
            null -> {}
        }

        BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
            when {
                state.selectionMode -> viewModel.clearSelection()
                state.searchQuery != null -> viewModel.search(null)
            }
        }

        LaunchedEffect(state.selectionMode, state.dialog) {
            HomeScreen.showBottomNav(!state.selectionMode)
        }

        LaunchedEffect(state.isLoading) {
            if (!state.isLoading) {
                (context as? MainActivity)?.ready = true
            }
        }

        LaunchedEffect(activeProfile?.id) {
            viewModel.closeDialog()
        }

        LaunchedEffect(Unit) {
            launch { queryEvent.receiveAsFlow().collect(viewModel::search) }
            launch {
                // Reselecting the tab first returns the page to its top; only a page already there opens the sheet.
                reselectEvent.receiveAsFlow().collectLatest {
                    if (!scrollToTopTarget.scrollToTop()) viewModel.showSettingsDialog()
                }
            }
            launch {
                viewModel.moveEvents.receiveAsFlow().collect { event ->
                    val message = when (event) {
                        is LibraryViewModel.MoveEvent.Success -> context.stringResource(
                            MR.strings.move_entries_success,
                            event.result.movedSelectedItemCount,
                            event.result.skippedSelectedItemCount,
                            event.result.overwrittenDuplicateCount,
                            event.result.removedSourceDuplicateCount,
                        )
                        LibraryViewModel.MoveEvent.Error -> context.stringResource(MR.strings.move_entries_failed)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    // For invoking search from other screen
    private val queryEvent = Channel<String>()
    suspend fun search(query: String) = queryEvent.send(query)

    // Tab reselection, handled by the visible page
    private val reselectEvent = Channel<Unit>()
}

@Composable
internal fun MergeLibraryEntriesSheet(
    dialog: LibraryViewModel.Dialog.MergeEntry,
    onDismissRequest: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onSelectTarget: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    val entries: PersistentList<MergeEditorEntry> = dialog.entries
        .map(LibraryViewModel.MergeEntry::toMergeEditorEntry)
        .toPersistentList()

    MergeEditorSheet(
        title = stringResource(MR.strings.action_merge),
        confirmText = stringResource(MR.strings.action_merge),
        entries = entries,
        targetId = dialog.targetId,
        targetLocked = dialog.targetLocked,
        onDismissRequest = onDismissRequest,
        onMove = onMove,
        onConfirm = onConfirm,
        onSelectTarget = onSelectTarget.takeUnless { dialog.targetLocked },
    )
}

private fun LibraryViewModel.MergeEntry.toMergeEditorEntry(): MergeEditorEntry {
    return MergeEditorEntry(
        id = id,
        entry = entry,
        subtitle = subtitle,
        isMember = isFromExistingMerge,
    )
}
