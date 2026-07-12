package eu.kanade.tachiyomi.ui.entry

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.domain.entry.model.hasCustomCover
import eu.kanade.presentation.browse.components.BrowseMergeEditorDialog
import eu.kanade.presentation.browse.components.MergeTargetPickerDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.NavigatorAdaptiveSheet
import eu.kanade.presentation.entry.DownloadAction
import eu.kanade.presentation.entry.EditCoverAction
import eu.kanade.presentation.entry.EntryChapterSettingsDialog
import eu.kanade.presentation.entry.EntryScreen
import eu.kanade.presentation.entry.components.DeleteChaptersDialog
import eu.kanade.presentation.entry.components.DuplicateEntryDialog
import eu.kanade.presentation.entry.components.EditDisplayNameDialog
import eu.kanade.presentation.entry.components.EntryCoverDialog
import eu.kanade.presentation.entry.components.EntryDownloadSettingsDialog
import eu.kanade.presentation.entry.components.ManageMergeDialog
import eu.kanade.presentation.entry.components.PreviewSizeUi
import eu.kanade.presentation.entry.components.ScanlatorFilterDialog
import eu.kanade.presentation.entry.components.SetIntervalDialog
import eu.kanade.presentation.library.DeleteLibraryEntriesDialog
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.source.entry.EntryCatalogueSource
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.entry.WebViewSource
import eu.kanade.tachiyomi.source.isLocalOrStub
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entry.track.TrackInfoDialogHomeScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.EntryOpenInteraction
import mihon.entry.interactions.EntryOpenOptions
import mihon.entry.interactions.EntryPreviewSize
import mihon.feature.migration.config.MigrationConfigScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.model.DuplicateEntryCandidate
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.screens.LoadingScreen
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class EntryScreen(
    private val entryId: Long,
    val fromSource: Boolean = false,
    private val bypassMerge: Boolean? = null,
) : Screen(), AssistContentScreen {

    private var assistUrl: String? = null

    override fun onProvideAssistUrl() = assistUrl

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) {
            LoadingScreen()
            return
        }

        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val lifecycleOwner = LocalLifecycleOwner.current
        val entryOpenInteraction = remember { Injekt.get<EntryOpenInteraction>() }
        val screenModel = rememberScreenModel {
            EntryScreenModel(
                context = context,
                lifecycle = lifecycleOwner.lifecycle,
                entryId = entryId,
                isFromSource = fromSource,
                bypassMerge = bypassMerge ?: false,
            )
        }

        val state by screenModel.state.collectAsStateWithLifecycle()

        if (state is EntryScreenModel.State.Loading) {
            LoadingScreen()
            return
        }

        if (state is EntryScreenModel.State.Error) {
            navigator.pop()
            return
        }

        val successState = state as EntryScreenModel.State.Success
        val previewConfig by screenModel.previewConfig.collectAsStateWithLifecycle()
        val previewState by screenModel.previewState.collectAsStateWithLifecycle()
        val webViewSource = remember(successState.source) { successState.source as? WebViewSource }

        LaunchedEffect(successState.entry, webViewSource) {
            if (webViewSource != null) {
                try {
                    withIOContext {
                        assistUrl = getEntryUrl(successState.entry, webViewSource)
                    }
                } catch (e: Exception) {
                    logcat(LogPriority.ERROR, e) { "Failed to get entry URL" }
                }
            }
        }

        EntryScreen(
            state = successState,
            snackbarHostState = screenModel.snackbarHostState,
            nextUpdate = successState.entry.expectedNextUpdate,
            isTabletUi = isTabletUi(),
            chapterSwipeStartAction = screenModel.chapterSwipeStartAction,
            chapterSwipeEndAction = screenModel.chapterSwipeEndAction,
            navigateUp = navigator::pop,
            onChapterClicked = { chapter ->
                scope.launch {
                    openChapter(context, entryOpenInteraction, successState.entry, chapter)
                }
            },
            onDownloadChapter = screenModel::runChapterDownloadActions.takeIf { !successState.source.isLocalOrStub() },
            onAddToLibraryClicked = {
                screenModel.toggleFavorite()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onAddToMergeClicked = screenModel::showMergeTargetPicker.takeIf {
                !successState.isPartOfMerge && (successState.isFromSource || successState.entry.favorite)
            },
            onWebViewClicked = {
                openEntryInWebView(
                    navigator,
                    successState.entry,
                    screenModel.source,
                )
            }.takeIf { webViewSource != null },
            onWebViewLongClicked = {
                copyEntryUrl(
                    context,
                    successState.entry,
                    screenModel.source,
                )
            }.takeIf { webViewSource != null },
            onTrackingClicked = {
                if (!successState.hasLoggedInTrackers) {
                    navigator.push(SettingsScreen(SettingsScreen.Destination.Tracking))
                } else {
                    screenModel.showTrackDialog()
                }
            },
            onDuplicatesClicked = screenModel::showDuplicateDialog.takeIf {
                successState.isFromSource &&
                    successState.entry.initialized &&
                    successState.duplicateCandidates.isNotEmpty()
            },
            onTagSearch = { scope.launch { performGenreSearch(navigator, it, screenModel.source) } },
            onFilterButtonClicked = screenModel::showSettingsDialog,
            onRefresh = screenModel::fetchAllFromSource,
            onContinueReading = {
                scope.launch {
                    continueReading(context, screenModel, successState.entry)
                }
            },
            onSearch = { query, global -> scope.launch { performSearch(navigator, query, global) } },
            onCoverClicked = screenModel::showCoverDialog,
            onShareClicked = { shareEntry(context, successState.entry, screenModel.source) }.takeIf {
                webViewSource != null
            },
            onDownloadActionClicked = screenModel::runDownloadAction
                .takeIf { !successState.source.isLocalOrStub() && screenModel.supportsBulkDownload() },
            onEditCategoryClicked = screenModel::showChangeCategoryDialog.takeIf { successState.entry.favorite },
            onEditFetchIntervalClicked = screenModel::showSetFetchIntervalDialog.takeIf {
                successState.entry.favorite
            },
            onEditDisplayNameClicked = screenModel::showEditDisplayNameDialog.takeIf { successState.entry.favorite },
            onManageMergeClicked = screenModel::showManageMergeDialog.takeIf { successState.isPartOfMerge },
            onOpenMergedEntryClicked = {
                navigator.push(EntryScreen(successState.mergeTargetId))
            }.takeIf { successState.showMergeNotice },
            onMigrateClicked = {
                navigator.push(MigrationConfigScreen(successState.entry.id))
            }.takeIf { successState.entry.favorite },
            onEditNotesClicked = {
                navigator.push(eu.kanade.tachiyomi.ui.entry.notes.EntryNotesScreen(entry = successState.entry))
            },
            onMultiBookmarkClicked = screenModel::bookmarkChapters,
            onMultiMarkAsReadClicked = screenModel::markChaptersRead,
            onMarkPreviousAsReadClicked = screenModel::markPreviousChapterRead,
            onMultiDeleteClicked = screenModel::showDeleteChapterDialog,
            onChapterSwipe = screenModel::chapterSwipe,
            onChapterSelected = screenModel::toggleSelection,
            onAllChapterSelected = screenModel::toggleAllSelection,
            onInvertSelection = screenModel::invertSelection,
            entryPreviewEnabled = screenModel.isEntryPreviewEnabled(successState.entry),
            entryPreviewSize = when (previewConfig.size) {
                EntryPreviewSize.SMALL -> PreviewSizeUi.SMALL
                EntryPreviewSize.MEDIUM -> PreviewSizeUi.MEDIUM
                EntryPreviewSize.LARGE -> PreviewSizeUi.LARGE
                EntryPreviewSize.EXTRA_LARGE -> PreviewSizeUi.EXTRA_LARGE
            },
            entryPreviewState = previewState,
            onPreviewExpandedChange = screenModel::setPreviewExpanded,
            onPreviewRetry = screenModel::retryPreview,
            onPreviewPageLoad = screenModel::loadPreviewPage,
            onPreviewPageClick = { chapterId, pageIndex ->
                scope.launch {
                    openChapter(
                        context,
                        entryOpenInteraction,
                        successState.entry,
                        successState.chapters.first {
                            it.chapter.id == chapterId
                        }.chapter,
                        pageIndex,
                    )
                }
            },
        )

        var showScanlatorsDialog by remember { mutableStateOf(false) }

        val onDismissRequest = { screenModel.dismissDialog() }
        when (val dialog = successState.dialog) {
            null -> {}
            is EntryScreenModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        screenModel.moveEntryToCategoriesAndAddToLibrary(dialog.entry, include)
                    },
                )
            }
            is EntryScreenModel.Dialog.DeleteChapters -> {
                DeleteChaptersDialog(
                    entryType = successState.entry.type,
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        screenModel.toggleAllSelection(false)
                        screenModel.deleteChapters(dialog.chapters)
                    },
                )
            }
            is EntryScreenModel.Dialog.DownloadSettings -> {
                EntryDownloadSettingsDialog(
                    selectedCount = dialog.items.size,
                    options = dialog.options,
                    onDismissRequest = onDismissRequest,
                    onConfirm = screenModel::confirmDownloadSettings,
                )
            }
            is EntryScreenModel.Dialog.DuplicateEntry -> {
                DuplicateEntryDialog(
                    duplicates = dialog.duplicates,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { screenModel.toggleFavorite(onRemoved = {}, checkDuplicate = false) },
                    onOpenEntry = { navigator.push(EntryScreen(it.id)) },
                    onMigrate = { screenModel.showMigrateDialog(it) },
                )
            }
            is EntryScreenModel.Dialog.EditDisplayName -> {
                EditDisplayNameDialog(
                    initialValue = dialog.initialValue,
                    onDismissRequest = onDismissRequest,
                    onConfirm = screenModel::updateDisplayName,
                )
            }
            is EntryScreenModel.Dialog.EditMerge -> {
                BrowseMergeEditorDialog(
                    entries = dialog.entries,
                    targetId = dialog.targetId,
                    targetLocked = dialog.targetLocked,
                    removedIds = dialog.removedIds,
                    libraryRemovalIds = dialog.libraryRemovalIds,
                    confirmEnabled = dialog.enabled,
                    onDismissRequest = onDismissRequest,
                    onMove = screenModel::moveMergeEntry,
                    onSelectTarget = screenModel::setMergeTarget,
                    onToggleRemove = screenModel::toggleMergeEntryRemoval,
                    onToggleLibraryRemove = screenModel::toggleMergeEntryLibraryRemoval,
                    onConfirm = screenModel::confirmMerge,
                )
            }
            is EntryScreenModel.Dialog.ManageMerge -> {
                ManageMergeDialog(
                    targetId = dialog.targetId,
                    members = dialog.members,
                    removableIds = dialog.removableIds,
                    libraryRemovalIds = dialog.libraryRemovalIds,
                    onDismissRequest = onDismissRequest,
                    onMove = screenModel::reorderMergeMembers,
                    onSaveOrder = screenModel::saveMergeOrder,
                    onOpenManga = { entryIdToOpen ->
                        screenModel.dismissDialog()
                        navigator.push(
                            EntryScreen(
                                entryIdToOpen,
                                bypassMerge = entryIdToOpen != dialog.savedTargetId,
                            ),
                        )
                    },
                    onSelectTarget = screenModel::setManageMergeTarget,
                    onToggleRemoveMember = screenModel::toggleMergedMemberRemoval,
                    onToggleRemoveMemberFromLibrary = screenModel::toggleMergedMemberLibraryRemoval,
                    onRemoveMembers = screenModel::removeMergedMembers,
                    onUnmergeAll = screenModel::unmergeAll,
                )
            }
            is EntryScreenModel.Dialog.RemoveMergedEntry -> {
                DeleteLibraryEntriesDialog(
                    containsLocalEntries = dialog.containsLocalEntry,
                    containsMergedEntries = true,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { deleteEntry, deleteChapter ->
                        screenModel.removeMergedEntry(dialog.members, deleteEntry, deleteChapter)
                    },
                )
            }
            is EntryScreenModel.Dialog.Migrate -> {
                // TODO(Phase 7.5): Add a type-aware migrate dialog for unified entries.
                // For now navigate to the migration config screen for the current entry.
                navigator.push(MigrationConfigScreen(dialog.current.id))
                screenModel.dismissDialog()
            }
            is EntryScreenModel.Dialog.SelectMergeTarget -> {
                MergeTargetPickerDialog(
                    title = context.stringResource(MR.strings.action_merge_into_library),
                    query = dialog.query,
                    visibleTargets = dialog.visibleTargets,
                    onDismissRequest = onDismissRequest,
                    onQueryChange = screenModel::updateMergeTargetQuery,
                    onSelectTarget = screenModel::openMergeEditor,
                )
            }
            EntryScreenModel.Dialog.SettingsSheet -> EntryChapterSettingsDialog(
                onDismissRequest = onDismissRequest,
                entry = successState.entry,
                isMerged = successState.isMerged,
                onDownloadFilterChanged = screenModel::setDownloadedFilter,
                onUnreadFilterChanged = screenModel::setUnreadFilter,
                onBookmarkedFilterChanged = screenModel::setBookmarkedFilter,
                onSortModeChanged = screenModel::setSorting,
                onDisplayModeChanged = screenModel::setDisplayMode,
                onSetAsDefault = screenModel::setCurrentSettingsAsDefault,
                onResetToDefault = screenModel::resetToDefaultSettings,
                scanlatorFilterActive = successState.scanlatorFilterActive,
                onScanlatorFilterClicked = {
                    showScanlatorsDialog = true
                }.takeIf { successState.childGroupFilterSupported },
            )
            EntryScreenModel.Dialog.TrackSheet -> {
                NavigatorAdaptiveSheet(
                    screen = TrackInfoDialogHomeScreen(
                        entryId = successState.entry.id,
                        entryTitle = successState.entry.displayTitle,
                        sourceId = successState.entry.source,
                        entryType = successState.entry.type,
                    ),
                    enableSwipeDismiss = { it.lastItem is TrackInfoDialogHomeScreen },
                    onDismissRequest = onDismissRequest,
                )
            }
            EntryScreenModel.Dialog.FullCover -> {
                val sm = rememberScreenModel { EntryCoverScreenModel(successState.entry.id) }
                val entry by sm.state.collectAsState()
                if (entry != null) {
                    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                        if (it == null) return@rememberLauncherForActivityResult
                        sm.editCover(context, it)
                    }
                    EntryCoverDialog(
                        coverData = entry!!,
                        snackbarHostState = sm.snackbarHostState,
                        isCustomCover = remember(entry) { entry!!.hasCustomCover() },
                        onShareClick = { sm.shareCover(context) },
                        onSaveClick = { sm.saveCover(context) },
                        onEditClick = {
                            when (it) {
                                EditCoverAction.EDIT -> getContent.launch("image/*")
                                EditCoverAction.DELETE -> sm.deleteCustomCover(context)
                            }
                        },
                        onDismissRequest = onDismissRequest,
                    )
                } else {
                    LoadingScreen(Modifier.systemBarsPadding())
                }
            }
            is EntryScreenModel.Dialog.SetFetchInterval -> {
                SetIntervalDialog(
                    interval = dialog.entry.fetchInterval,
                    nextUpdate = dialog.entry.expectedNextUpdate,
                    entryType = dialog.entry.type,
                    onDismissRequest = onDismissRequest,
                    onValueChanged = { interval: Int -> screenModel.setFetchInterval(dialog.entry, interval) }
                        .takeIf { screenModel.isUpdateIntervalEnabled },
                )
            }
        }

        if (showScanlatorsDialog && successState.childGroupFilterSupported) {
            ScanlatorFilterDialog(
                availableScanlators = successState.availableScanlators,
                excludedScanlators = successState.excludedScanlators,
                onDismissRequest = { showScanlatorsDialog = false },
                onConfirm = screenModel::setExcludedScanlators,
            )
        }
    }

    private suspend fun continueReading(context: Context, screenModel: EntryScreenModel, entry: Entry) {
        screenModel.continueEntry(context, entry)
    }

    private suspend fun openChapter(
        context: Context,
        entryOpenInteraction: EntryOpenInteraction,
        entry: Entry,
        chapter: EntryChapter,
        pageIndex: Int? = null,
    ) {
        entryOpenInteraction.open(
            context = context,
            entry = entry,
            chapter = chapter,
            options = EntryOpenOptions(pageIndex = pageIndex),
        )
    }

    private fun getEntryUrl(entry_: Entry?, source_: UnifiedSource?): String? {
        val entry = entry_ ?: return null
        val source = source_ as? WebViewSource ?: return null

        return try {
            source.getContentUrl(entry.toSEntry())
        } catch (e: Exception) {
            null
        }
    }

    private fun openEntryInWebView(navigator: Navigator, entry_: Entry?, source_: UnifiedSource?) {
        val source = source_ as? WebViewSource ?: return
        getEntryUrl(entry_, source_)?.let { url ->
            navigator.push(
                WebViewScreen(
                    url = url,
                    initialTitle = entry_?.title,
                    sourceId = source.id,
                    headers = source.getWebViewHeaders(),
                ),
            )
        }
    }

    private fun shareEntry(context: Context, entry_: Entry?, source_: UnifiedSource?) {
        try {
            getEntryUrl(entry_, source_)?.let { url ->
                val intent = url.toUri().toShareIntent(context, type = "text/plain")
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            context.toast(e.message)
        }
    }

    /**
     * Perform a search using the provided query.
     */
    private suspend fun performSearch(navigator: Navigator, query: String, global: Boolean) {
        if (global) {
            navigator.push(GlobalSearchScreen(query))
            return
        }

        if (navigator.size < 2) {
            return
        }

        when (val previousController = navigator.items[navigator.size - 2]) {
            is HomeScreen -> {
                navigator.pop()
                previousController.search(query)
            }
            is CatalogScreen -> {
                navigator.pop()
                previousController.search(query)
            }
        }
    }

    /**
     * Performs a genre search using the provided genre name.
     */
    private suspend fun performGenreSearch(navigator: Navigator, genreName: String, source: UnifiedSource?) {
        if (navigator.size < 2) {
            return
        }

        val previousController = navigator.items[navigator.size - 2]
        if (previousController is CatalogScreen && source is EntryCatalogueSource) {
            navigator.pop()
            previousController.searchGenre(genreName)
        } else {
            performSearch(navigator, genreName, global = false)
        }
    }

    /**
     * Copy Entry URL to Clipboard
     */
    private fun copyEntryUrl(context: Context, entry_: Entry?, source_: UnifiedSource?) {
        val url = getEntryUrl(entry_, source_) ?: return
        context.copyToClipboard(url, url)
    }
}
