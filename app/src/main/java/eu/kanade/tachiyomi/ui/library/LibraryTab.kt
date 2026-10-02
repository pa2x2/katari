package eu.kanade.tachiyomi.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.fragment.app.FragmentActivity
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.components.LibraryBottomActionMenu
import eu.kanade.presentation.entry.components.MergeEditorDialog
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.selectionEntryTypePresentation
import eu.kanade.presentation.library.DeleteLibraryEntriesDialog
import eu.kanade.presentation.library.EmptyLibraryScreen
import eu.kanade.presentation.library.LibrarySettingsDialog
import eu.kanade.presentation.library.MoveEntriesCategoryDialog
import eu.kanade.presentation.library.MoveEntriesConflictDialog
import eu.kanade.presentation.library.MoveEntriesProfileDialog
import eu.kanade.presentation.library.components.LibraryContent
import eu.kanade.presentation.library.components.LibraryScrollToTopTarget
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.library.components.LibraryUpdateModeDialog
import eu.kanade.presentation.library.update.rememberLibraryUpdateStarter
import eu.kanade.presentation.more.onboarding.GETTING_STARTED_URL
import eu.kanade.presentation.more.settings.screen.data.rememberRestoreBackupLauncher
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
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
import mihon.entry.interactions.navigation.EntryContinueFeature
import mihon.feature.migration.config.MigrationConfigScreen
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.runtime.collectAsState as collectFlowAsState

data object LibraryTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 0u,
                title = stringResource(MR.strings.label_library),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        reselectEvent.send(Unit)
    }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val haptic = LocalHapticFeedback.current
        val profileManager = remember { Injekt.get<ProfileManager>() }
        val activeProfile by profileManager.activeProfile.collectFlowAsState()
        val visibleProfiles by profileManager.visibleProfiles.collectFlowAsState()
        val entryContinueFeature = remember { Injekt.get<EntryContinueFeature>() }

        val screenModel = rememberScreenModel { LibraryScreenModel(context.applicationContext) }
        val settingsScreenModel =
            rememberScreenModel(tag = activeProfile?.id?.toString()) { LibrarySettingsScreenModel() }
        val state by screenModel.state.collectAsState()
        val updateProgress by screenModel.updateProgress.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }
        val scrollToTopTarget = remember { LibraryScrollToTopTarget() }

        // A started update also reports its progress on the library's progress strip.
        val updateStarter = rememberLibraryUpdateStarter(snackbarHostState) {
            LibraryUpdateJob.startSkippedOfLatest(context)
        }

        val onClickRefresh: suspend (LibraryScreenModel.State) -> Boolean = { state ->
            val activePage = state.activePage
            updateStarter.start {
                LibraryUpdateJob.startNow(
                    context = context,
                    category = activePage?.category,
                    sourceId = activePage?.sourceId,
                    entryType = activePage?.entryType,
                )
            }
        }

        val onClickGlobalUpdate: suspend () -> Boolean = {
            updateStarter.start { LibraryUpdateJob.startNow(context) }
        }

        Scaffold(
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
                    onClickUnselectAll = screenModel::clearSelection,
                    onClickSelectAll = screenModel::selectAll,
                    onClickInvertSelection = screenModel::invertSelection,
                    onClickFilter = screenModel::showSettingsDialog,
                    onClickRefresh = { scope.launch { onClickRefresh(state) } },
                    onClickGlobalUpdate = { scope.launch { onClickGlobalUpdate() } },
                    onClickOpenRandomEntry = {
                        scope.launch {
                            val randomItem = screenModel.getRandomLibraryItemForCurrentPage()
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
                    onSearchQueryChange = screenModel::search,
                    // For scroll overlay when no tab
                    scrollBehavior = scrollBehavior.takeIf { !state.showCategoryTabs },
                )
            },
            bottomBar = {
                val actionLabels = state.selectedEntryTypes.entrySelectionActionLabels()
                val pinAction = state.selectionPinAction
                LibraryBottomActionMenu(
                    visible = state.selectionMode,
                    onPinClicked = screenModel::setSelectionPinned
                        .takeUnless { pinAction == LibraryPinSelectionAction.Hidden },
                    pinSelection = pinAction == LibraryPinSelectionAction.Pin,
                    onMergeClicked = screenModel::openMergeDialog.takeIf { screenModel.isMergeSelectionAvailable() },
                    onChangeCategoryClicked = screenModel::openChangeCategoryDialog,
                    onMarkAsReadClicked = { screenModel.markReadSelection(true) }
                        .takeIf { screenModel.canSetConsumedSelection() },
                    onMarkAsUnreadClicked = { screenModel.markReadSelection(false) }
                        .takeIf { screenModel.canSetConsumedSelection() },
                    markAsReadLabel = actionLabels.markAsReadLabel,
                    markAsUnreadLabel = actionLabels.markAsUnreadLabel,
                    downloadPresentation = state.selectedEntryTypes.selectionEntryTypePresentation(),
                    bookmarkedDownloadsSupported = screenModel.canDownloadSelection(
                        DownloadAction.BOOKMARKED_CHAPTERS,
                    ),
                    onDownloadClicked = screenModel::performDownloadAction
                        .takeIf { screenModel.canDownloadSelection() },
                    onDeleteClicked = screenModel::openDeleteEntriesDialog,
                    onMigrateClicked = {
                        val selection = screenModel.selectedMigrationSubjects()
                        screenModel.clearSelection()
                        navigator.push(MigrationConfigScreen(selection))
                    }.takeIf { screenModel.canMigrateSelection() },
                    onMoveToProfileClicked = screenModel::openMoveProfileDialog
                        .takeIf { visibleProfiles.any { it.id != activeProfile?.id } },
                    onCheckForUpdatesClicked = {
                        val entryIds = screenModel.takeSelectionForUpdate()
                        scope.launch { updateStarter.start { LibraryUpdateJob.startSelection(context, entryIds) } }
                    },
                    onUpdateModeClicked = screenModel::openUpdateModeDialog,
                )
            },
            snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
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
                                screenModel.updateActivePageIndex(profileId, index)
                            }
                        },
                        onClickItem = { item ->
                            navigator.push(EntryScreen(item.entry.id))
                        },
                        onContinueReadingClicked = { item: LibraryItem ->
                            scope.launchIO {
                                entryContinueFeature.continueEntry(context, item.entry)
                            }
                            Unit
                        }.takeIf { state.showContinueButton },
                        isContinueReadingAvailable = { item ->
                            entryContinueFeature.isApplicable(item.entry.type)
                        },
                        onToggleSelection = screenModel::toggleSelection,
                        onToggleRangeSelection = { page, item ->
                            screenModel.toggleRangeSelection(page, item)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onRefresh = { onClickRefresh(state) },
                        updateProgress = updateProgress,
                        onCancelUpdate = screenModel::cancelUpdate,
                        onGlobalSearchClicked = {
                            navigator.push(GlobalSearchScreen(screenModel.state.value.searchQuery ?: ""))
                        },
                        getDisplayMode = { screenModel.getDisplayMode() },
                        getColumnsForOrientation = { screenModel.getColumnsForOrientation(it) },
                        getItemsForPage = { state.getItemsForPage(it) },
                        displaySettingsForPage = state::displaySettingsForPage,
                        scrollToTopTarget = scrollToTopTarget,
                        onSearchQueryChange = screenModel::search,
                        onSeeAllSearchResults = { page ->
                            // Leave search on the chosen group's page; the model maps it onto the unsearched pages.
                            val index = state.displayedPages.indexOfFirst { it.id == page.id }
                            val profileId = state.libraryData.profileId
                            if (index >= 0 && profileId != null) screenModel.updateActivePageIndex(profileId, index)
                            screenModel.search(null)
                        },
                    )
                }
            }
        }

        val onDismissRequest = screenModel::closeDialog
        when (val dialog = state.dialog) {
            is LibraryScreenModel.Dialog.SettingsSheet -> run {
                LibrarySettingsDialog(
                    onDismissRequest = onDismissRequest,
                    screenModel = settingsScreenModel,
                    category = state.activeSortCategory,
                    filterAvailability = state.libraryData.filterAvailability,
                )
            }
            is LibraryScreenModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = {
                        screenModel.clearSelection()
                        navigator.push(CategoryScreen())
                    },
                    onConfirm = { include, exclude ->
                        screenModel.clearSelection()
                        screenModel.setEntryCategories(dialog.items, include, exclude)
                    },
                )
            }
            is LibraryScreenModel.Dialog.DeleteEntries -> {
                DeleteLibraryEntriesDialog(
                    containsLocalEntries = dialog.containsLocalEntries,
                    containsMergedEntries = dialog.containsMergedEntries,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { deleteFromLibrary, deleteChapter ->
                        screenModel.removeEntries(dialog.entries, deleteFromLibrary, deleteChapter)
                        screenModel.clearSelection()
                    },
                )
            }
            is LibraryScreenModel.Dialog.MergeEntry -> {
                MergeLibraryEntriesDialog(
                    dialog = dialog,
                    onDismissRequest = onDismissRequest,
                    onMove = screenModel::reorderMergeSelection,
                    onSelectTarget = screenModel::setMergeTarget,
                    onConfirm = screenModel::confirmMergeSelection,
                )
            }
            is LibraryScreenModel.Dialog.UpdateMode -> {
                LibraryUpdateModeDialog(
                    entryCount = dialog.entryIds.size,
                    currentMode = dialog.currentMode,
                    onDismissRequest = onDismissRequest,
                    onModeSelected = { mode -> screenModel.setUpdateMode(dialog.entryIds, mode) },
                )
            }
            is LibraryScreenModel.Dialog.MoveProfile -> {
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
                                screenModel.openMoveCategoryDialog(profile)
                            }
                        }
                    },
                )
            }
            is LibraryScreenModel.Dialog.MoveCategory -> {
                MoveEntriesCategoryDialog(
                    categories = dialog.categories,
                    onDismissRequest = onDismissRequest,
                    onCategorySelected = { categoryId ->
                        screenModel.prepareMoveToProfile(dialog.profile, categoryId)
                    },
                )
            }
            is LibraryScreenModel.Dialog.MoveConflict -> {
                val conflict = dialog.preview.conflicts[dialog.conflictIndex]
                MoveEntriesConflictDialog(
                    conflict = conflict,
                    conflictNumber = dialog.conflictIndex + 1,
                    conflictCount = dialog.preview.conflicts.size,
                    destinationProfileName = dialog.profile.name,
                    sourceName = screenModel.getSourceDisplayName(conflict.sourceEntry.source),
                    onDismissRequest = onDismissRequest,
                    onResolve = screenModel::resolveMoveConflict,
                )
            }
            null -> {}
        }

        BackHandler(enabled = state.selectionMode || state.searchQuery != null) {
            when {
                state.selectionMode -> screenModel.clearSelection()
                state.searchQuery != null -> screenModel.search(null)
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
            screenModel.closeDialog()
        }

        LaunchedEffect(Unit) {
            launch { queryEvent.receiveAsFlow().collect(screenModel::search) }
            launch {
                // Reselecting the tab first returns the page to its top; only a page already there opens the sheet.
                reselectEvent.receiveAsFlow().collectLatest {
                    if (!scrollToTopTarget.scrollToTop()) screenModel.showSettingsDialog()
                }
            }
            launch {
                screenModel.moveEvents.receiveAsFlow().collect { event ->
                    val message = when (event) {
                        is LibraryScreenModel.MoveEvent.Success -> context.stringResource(
                            MR.strings.move_entries_success,
                            event.result.movedSelectedItemCount,
                            event.result.skippedSelectedItemCount,
                            event.result.overwrittenDuplicateCount,
                            event.result.removedSourceDuplicateCount,
                        )
                        LibraryScreenModel.MoveEvent.Error -> context.stringResource(MR.strings.move_entries_failed)
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
internal fun MergeLibraryEntriesDialog(
    dialog: LibraryScreenModel.Dialog.MergeEntry,
    onDismissRequest: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onSelectTarget: (Long) -> Unit,
    onConfirm: () -> Unit,
) {
    val entries: PersistentList<MergeEditorEntry> = dialog.entries
        .map(LibraryScreenModel.MergeEntry::toMergeEditorEntry)
        .toPersistentList()

    MergeEditorDialog(
        title = stringResource(MR.strings.action_merge),
        entries = entries,
        targetId = dialog.targetId,
        targetLocked = dialog.targetLocked,
        onDismissRequest = onDismissRequest,
        onMove = onMove,
        onConfirm = onConfirm,
        onSelectTarget = onSelectTarget.takeUnless { dialog.targetLocked },
    )
}

private fun LibraryScreenModel.MergeEntry.toMergeEditorEntry(): MergeEditorEntry {
    return MergeEditorEntry(
        id = id,
        entry = entry,
        subtitle = subtitle,
        isMember = isFromExistingMerge,
    )
}
