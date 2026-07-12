package eu.kanade.tachiyomi.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
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
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.presentation.entry.components.LibraryBottomActionMenu
import eu.kanade.presentation.entry.components.MergeEditorDialog
import eu.kanade.presentation.entry.components.MergeEditorEntry
import eu.kanade.presentation.entry.selectionEntryTypePresentation
import eu.kanade.presentation.library.DeleteLibraryEntriesDialog
import eu.kanade.presentation.library.LibrarySettingsDialog
import eu.kanade.presentation.library.MoveEntriesCategoryDialog
import eu.kanade.presentation.library.MoveEntriesConflictDialog
import eu.kanade.presentation.library.MoveEntriesProfileDialog
import eu.kanade.presentation.library.components.LibraryContent
import eu.kanade.presentation.library.components.LibraryToolbar
import eu.kanade.presentation.more.onboarding.GETTING_STARTED_URL
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.entry.entrySelectionActionLabels
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.system.AuthenticatorUtil.authenticate
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryContinueInteraction
import mihon.feature.migration.config.MigrationConfigScreen
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
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
        requestOpenSettingsSheet()
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
        val entryContinueInteraction = remember { Injekt.get<EntryContinueInteraction>() }

        val screenModel = rememberScreenModel { LibraryScreenModel(context.applicationContext) }
        val settingsScreenModel =
            rememberScreenModel(tag = activeProfile?.id?.toString()) { LibrarySettingsScreenModel() }
        val state by screenModel.state.collectAsState()

        val snackbarHostState = remember { SnackbarHostState() }

        fun showRefreshMessage(started: Boolean, messageRes: StringResource) {
            scope.launch {
                val msgRes = when {
                    !started -> MR.strings.update_already_running
                    else -> messageRes
                }
                snackbarHostState.showSnackbar(context.stringResource(msgRes))
            }
        }

        val onClickRefresh: (LibraryScreenModel.State) -> Boolean = { state ->
            val activePage = state.activePage
            val started = LibraryUpdateJob.startNow(
                context = context,
                category = activePage?.category,
                sourceId = activePage?.sourceId,
                entryType = activePage?.entryType,
            )
            val activeConstraints = listOf(
                activePage?.category,
                activePage?.sourceId,
                activePage?.entryType,
            ).count { it != null }
            val messageRes = when {
                activeConstraints > 1 -> MR.strings.updating_group
                activePage?.entryType != null -> MR.strings.updating_type
                activePage?.sourceId != null -> MR.strings.updating_extension
                activePage?.category != null -> MR.strings.updating_category
                else -> MR.strings.updating_library
            }
            showRefreshMessage(started, messageRes)
            started
        }

        val onClickGlobalUpdate: () -> Boolean = {
            val started = LibraryUpdateJob.startNow(context)
            showRefreshMessage(started, MR.strings.updating_library)
            started
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
                    currentGroupType = state.groupType,
                    onClickUnselectAll = screenModel::clearSelection,
                    onClickSelectAll = screenModel::selectAll,
                    onClickInvertSelection = screenModel::invertSelection,
                    onClickFilter = screenModel::showSettingsDialog,
                    onClickRefresh = { onClickRefresh(state) },
                    onClickGlobalUpdate = { onClickGlobalUpdate() },
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
                LibraryBottomActionMenu(
                    visible = state.selectionMode,
                    onMergeClicked = screenModel::openMergeDialog.takeIf { screenModel.canMergeSelection() },
                    onChangeCategoryClicked = screenModel::openChangeCategoryDialog,
                    onMarkAsReadClicked = { screenModel.markReadSelection(true) },
                    onMarkAsUnreadClicked = { screenModel.markReadSelection(false) },
                    markAsReadLabel = actionLabels.markAsReadLabel,
                    markAsUnreadLabel = actionLabels.markAsUnreadLabel,
                    downloadPresentation = state.selectedEntryTypes.selectionEntryTypePresentation(),
                    onDownloadClicked = screenModel::performDownloadAction
                        .takeIf { screenModel.canDownloadSelection() },
                    onDeleteClicked = screenModel::openDeleteEntriesDialog,
                    onMigrateClicked = {
                        val selection = screenModel.selectedMigrationEntryIds()
                        screenModel.clearSelection()
                        navigator.push(MigrationConfigScreen(selection))
                    }.takeIf { screenModel.canMigrateSelection() },
                    onMoveToProfileClicked = screenModel::openMoveProfileDialog
                        .takeIf { visibleProfiles.any { it.id != activeProfile?.id } },
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
                    EmptyScreen(
                        stringRes = MR.strings.information_empty_library,
                        modifier = Modifier.padding(contentPadding),
                        actions = listOf(
                            EmptyScreenAction(
                                stringRes = MR.strings.getting_started_guide,
                                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                onClick = { handler.openUri(GETTING_STARTED_URL) },
                            ),
                        ),
                    )
                }
                else -> {
                    LibraryContent(
                        pages = state.displayedPages,
                        searchQuery = state.searchQuery,
                        selection = state.selection,
                        contentPadding = contentPadding,
                        currentPage = state.coercedActivePageIndex,
                        hasActiveFilters = state.hasActiveFilters,
                        showPageTabs = state.showCategoryTabs || !state.searchQuery.isNullOrEmpty(),
                        onChangeCurrentPage = screenModel::updateActivePageIndex,
                        onClickItem = { item ->
                            navigator.push(EntryScreen(item.entry.id))
                        },
                        onContinueReadingClicked = { item: LibraryItem ->
                            scope.launchIO {
                                entryContinueInteraction.continueEntry(context, item.entry)
                            }
                            Unit
                        }.takeIf { state.showContinueButton },
                        onToggleSelection = screenModel::toggleSelection,
                        onToggleRangeSelection = { page, item ->
                            screenModel.toggleRangeSelection(page, item)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onRefresh = { onClickRefresh(state) },
                        onGlobalSearchClicked = {
                            navigator.push(GlobalSearchScreen(screenModel.state.value.searchQuery ?: ""))
                        },
                        getItemCountForPage = { state.getItemCountForPage(it) },
                        getItemCountForPrimaryTab = { state.getItemCountForPrimaryTab(it) },
                        getDisplayMode = { screenModel.getDisplayMode() },
                        getColumnsForOrientation = { screenModel.getColumnsForOrientation(it) },
                        getItemsForPage = { state.getItemsForPage(it) },
                        displaySettings = state.displaySettings,
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
            is LibraryScreenModel.Dialog.MoveProfile -> {
                MoveEntriesProfileDialog(
                    profiles = dialog.profiles,
                    onDismissRequest = onDismissRequest,
                    onProfileSelected = { profile ->
                        scope.launch {
                            val authenticated = if (profileManager.profileRequiresUnlock(profile.id)) {
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
            launch { requestSettingsSheetEvent.receiveAsFlow().collectLatest { screenModel.showSettingsDialog() } }
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

    // For opening settings sheet in LibraryController
    private val requestSettingsSheetEvent = Channel<Unit>()
    private suspend fun requestOpenSettingsSheet() = requestSettingsSheetEvent.send(Unit)
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
