package eu.kanade.tachiyomi.ui.entry

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.presentation.browse.components.BrowseMergeEditorSheet
import eu.kanade.presentation.browse.components.MergeTargetPickerDialog
import eu.kanade.presentation.browse.components.RemoveMangaDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.presentation.components.NavigatorAdaptiveSheet
import eu.kanade.presentation.entry.EditCoverAction
import eu.kanade.presentation.entry.EntryChapterSettingsDialog
import eu.kanade.presentation.entry.EntryScreen
import eu.kanade.presentation.entry.activity.EntryActivitySheet
import eu.kanade.presentation.entry.components.DeleteChaptersDialog
import eu.kanade.presentation.entry.components.DuplicateEntrySheet
import eu.kanade.presentation.entry.components.EditDisplayNameDialog
import eu.kanade.presentation.entry.components.EntryCoverDialog
import eu.kanade.presentation.entry.components.EntryDownloadSettingsDialog
import eu.kanade.presentation.entry.components.ManageMergeSheet
import eu.kanade.presentation.entry.components.ScanlatorFilterDialog
import eu.kanade.presentation.entry.components.SetIntervalDialog
import eu.kanade.presentation.entry.components.preview.PreviewSizeUi
import eu.kanade.presentation.entry.translation.EntryChapterTranslationUi
import eu.kanade.presentation.entry.translation.EntryTranslateSetupSheet
import eu.kanade.presentation.entry.translation.TranslatableChapter
import eu.kanade.presentation.entry.updates.EntryUpdatesSheet
import eu.kanade.presentation.entry.updates.daysUntilRelease
import eu.kanade.presentation.entry.updates.entryReleaseEstimateText
import eu.kanade.presentation.entry.updates.entryUpdatesLabel
import eu.kanade.presentation.library.DeleteLibraryEntriesDialog
import eu.kanade.presentation.util.AssistContentScreen
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.data.coil.SourceImage
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.ui.browse.catalog.CatalogScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.category.CategoryScreen
import eu.kanade.tachiyomi.ui.entry.activity.EntryActivityViewModel
import eu.kanade.tachiyomi.ui.entry.related.RelatedEntriesDialog
import eu.kanade.tachiyomi.ui.entry.related.RelatedEntriesViewModel
import eu.kanade.tachiyomi.ui.entry.track.TrackInfoDialogHomeScreen
import eu.kanade.tachiyomi.ui.entry.updates.EntryUpdatesViewModel
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import eu.kanade.tachiyomi.util.system.copyToClipboard
import eu.kanade.tachiyomi.util.system.isReleaseBuildType
import eu.kanade.tachiyomi.util.system.toShareIntent
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.app.di.appGraph
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.entry.interactions.child.EntryRelatedEntriesAvailability
import mihon.entry.interactions.child.EntryRelatedEntriesContext
import mihon.entry.interactions.child.EntryScanlatorFilterStateResult
import mihon.entry.interactions.download.EntryBulkDownloadAction
import mihon.entry.interactions.download.EntryDownloadActionAvailability
import mihon.entry.interactions.download.EntryDownloadActionRequest
import mihon.entry.interactions.media.EntryPreviewOpenTargetResult
import mihon.entry.interactions.media.EntryPreviewSize
import mihon.entry.interactions.navigation.EntryOpenFeature
import mihon.entry.interactions.navigation.EntryOpenOptions
import mihon.entry.interactions.source.EntryWebViewResolution
import mihon.feature.migration.config.MigrationConfigScreen
import mihon.feature.migration.dialog.MigrateEntryDialog
import mihon.presentation.kit.feedback.LoadingScreen
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.i18n.context.stringResource
import tachiyomi.source.local.isLocal
import java.time.LocalDate

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
        val graph = remember { context.appGraph }
        val entryOpenFeature = remember { graph.entryOpenFeature }
        val entryConsumptionFeature = remember { graph.entryConsumptionFeature }
        val entryDownloadActionFeature = remember { graph.entryDownloadActionFeature }
        val entryBookmarkFeature = remember { graph.entryBookmarkFeature }
        val entryRelatedEntriesFeature = remember { graph.entryRelatedEntriesFeature }
        val entryWebViewFeature = remember { graph.entryWebViewFeature }
        val entryCatalogueFeature = remember { graph.entryCatalogueFeature }
        val viewModel = assistedMetroViewModel<EntryViewModel, EntryViewModel.Factory> {
            create(
                context = context,
                entryId = entryId,
                isFromSource = fromSource,
                bypassMerge = bypassMerge ?: false,
            )
        }
        val relatedEntriesViewModel = assistedMetroViewModel<RelatedEntriesViewModel, RelatedEntriesViewModel.Factory>(
            key = "related-entries-$entryId",
        ) {
            create(entryId)
        }

        val state by viewModel.state.collectAsStateWithLifecycle()

        if (state is EntryViewModel.State.Loading) {
            LoadingScreen()
            return
        }

        if (state is EntryViewModel.State.Error) {
            navigator.pop()
            return
        }

        val successState = state as EntryViewModel.State.Success
        val downloadRequest = remember(successState.entry.type, successState.downloadSourceIds) {
            EntryDownloadActionRequest(
                type = successState.entry.type,
                sourceIds = successState.downloadSourceIds + successState.entry.source,
            )
        }
        val downloadCapabilities = remember(downloadRequest) {
            Triple(
                entryDownloadActionFeature.individualAvailability(downloadRequest) ==
                    EntryDownloadActionAvailability.Available,
                entryDownloadActionFeature.bulkAvailability(
                    requests = listOf(downloadRequest),
                    action = EntryBulkDownloadAction.unread,
                ) == EntryDownloadActionAvailability.Available,
                entryDownloadActionFeature.bulkAvailability(
                    requests = listOf(downloadRequest),
                    action = EntryBulkDownloadAction.bookmarked,
                ) == EntryDownloadActionAvailability.Available,
            )
        }
        val downloadsAvailable = downloadCapabilities.first
        val bulkDownloadsAvailable = downloadCapabilities.second
        val bookmarksSupported = entryBookmarkFeature.isApplicable(successState.entry.type)
        val bookmarkedDownloadsSupported = downloadCapabilities.third
        val translationEntries = remember(successState.chapters.map { it.entry.id }.distinct()) {
            successState.chapters.map { it.entry }.distinctBy { it.id }
        }
        val translationStatuses by remember(translationEntries) {
            viewModel.translation.statuses(translationEntries)
        }.collectAsStateWithLifecycle(emptyMap())
        val translationUi = remember(translationStatuses) {
            EntryChapterTranslationUi(translationStatuses) { items, action ->
                val chapters = items.map { TranslatableChapter(it.entry, it.chapter, it.downloadState) }
                viewModel.translation.run(chapters, action, translationStatuses)
            }
        }.takeIf { downloadsAvailable && translationEntries.any { viewModel.translation.isApplicable(it.type) } }
        val translationSetup by viewModel.translation.sheet.collectAsStateWithLifecycle()
        val previewConfig by viewModel.preview.config.collectAsStateWithLifecycle()
        val previewState by viewModel.preview.previewState.collectAsStateWithLifecycle()
        val webView = remember(successState.entry) {
            entryWebViewFeature.resolveEntry(successState.entry)
        }
        val availableWebView = webView as? EntryWebViewResolution.Available
        val relatedEntriesAvailability = remember(successState.entry, successState.source) {
            entryRelatedEntriesFeature.availability(
                EntryRelatedEntriesContext(
                    entry = successState.entry,
                    source = successState.source,
                ),
            )
        }
        var showRelatedEntriesDialog by rememberSaveable(successState.entry.id) { mutableStateOf(false) }
        val openApplicable = entryOpenFeature.isApplicable(successState.entry.type)
        val consumptionApplicable = entryConsumptionFeature.isApplicable(successState.entry.type)

        val updatesViewModel = assistedMetroViewModel<EntryUpdatesViewModel, EntryUpdatesViewModel.Factory>(
            key = "updates-${successState.entry.id}",
        ) {
            create(successState.entry.id, successState.memberIds)
        }
        val updatesState by updatesViewModel.state.collectAsStateWithLifecycle()
        var showUpdatesSheet by rememberSaveable(successState.entry.id) { mutableStateOf(false) }
        val nextUpdateDays = remember(successState.entry.expectedNextUpdate) {
            daysUntilRelease(successState.entry.expectedNextUpdate)
        }
        val updatesLabel = entryUpdatesLabel(
            mode = updatesState.mode,
            status = updatesState.status,
            nextUpdateDays = nextUpdateDays,
            isUserInterval = successState.entry.fetchInterval < 0,
        )
        if (showUpdatesSheet) {
            EntryUpdatesSheet(
                entry = successState.entry,
                mode = updatesState.mode,
                status = updatesState.status,
                categories = updatesState.categories,
                pausedSources = updatesState.pausedSources,
                releaseEstimate = entryReleaseEstimateText(
                    interval = successState.entry.fetchInterval,
                    nextUpdate = successState.entry.expectedNextUpdate,
                    entryType = successState.entry.type,
                ),
                onModeSelected = updatesViewModel::setMode,
                onResumeSource = updatesViewModel::resumeSource,
                onReleaseEstimateClicked = {
                    showUpdatesSheet = false
                    viewModel.showSetFetchIntervalDialog()
                }.takeIf { !isReleaseBuildType && viewModel.isUpdateIntervalEnabled },
                onDismissRequest = { showUpdatesSheet = false },
            )
        }

        val activityViewModel = assistedMetroViewModel<EntryActivityViewModel, EntryActivityViewModel.Factory>(
            key = "activity-${successState.entry.id}-${successState.memberIds.joinToString(",")}",
        ) {
            create(successState.entry.profileId, successState.entry.type, successState.memberIds)
        }
        val activityState by activityViewModel.state.collectAsStateWithLifecycle()
        var showActivitySheet by rememberSaveable(successState.entry.id) { mutableStateOf(false) }
        val activitySummary = activityState.summary
        if (showActivitySheet && activitySummary != null) {
            EntryActivitySheet(
                summary = activitySummary,
                progress = successState.chapterProgress,
                consumedUnitLabel = activityViewModel.consumedUnitLabel,
                paceMillis = activityState.paceMillis,
                onOpenActivity = activitySummary.firstActivityLocalDate?.let { firstDate ->
                    {
                        showActivitySheet = false
                        navigator.push(
                            HistoryActivityScreen(
                                startLocalDate = firstDate,
                                endLocalDate = LocalDate.now().toString(),
                                entryIds = successState.memberIds.toList(),
                                entryTitle = successState.entry.displayTitle,
                            ),
                        )
                    }
                },
                onDismissRequest = { showActivitySheet = false },
            )
        }

        LaunchedEffect(webView) {
            assistUrl = availableWebView?.url
            if (webView is EntryWebViewResolution.Failed) {
                logcat(LogPriority.ERROR, webView.cause) { "Failed to get entry URL" }
            }
        }

        EntryScreen(
            state = successState,
            snackbarHostState = viewModel.snackbarHostState,
            updatesLabel = updatesLabel,
            activity = activityState,
            onActivityClicked = { showActivitySheet = true },
            isTabletUi = isTabletUi(),
            chapterSwipeStartAction = viewModel.chapterSwipeStartAction.availableFor(
                downloadsSupported = downloadsAvailable,
                bookmarksSupported = bookmarksSupported,
                consumptionSupported = consumptionApplicable,
            ),
            chapterSwipeEndAction = viewModel.chapterSwipeEndAction.availableFor(
                downloadsSupported = downloadsAvailable,
                bookmarksSupported = bookmarksSupported,
                consumptionSupported = consumptionApplicable,
            ),
            navigateUp = navigator::pop,
            onChapterClicked = { chapter: EntryChapter ->
                scope.launch {
                    openChapter(context, entryOpenFeature, successState.entry, chapter)
                }
                Unit
            }.takeIf { openApplicable },
            onDownloadChapter = viewModel::runChapterDownloadActions.takeIf { downloadsAvailable },
            downloadProgressProvider = viewModel::getDownloadProgress,
            translation = translationUi,
            onAddToLibraryClicked = {
                viewModel.toggleFavorite()
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            },
            onAddToMergeClicked = viewModel::showMergeTargetPicker.takeIf {
                !successState.isPartOfMerge &&
                    (successState.isFromSource || successState.entry.favorite)
            },
            onWebViewClicked = {
                openEntryInWebView(navigator, successState.entry, checkNotNull(availableWebView))
            }.takeIf { availableWebView != null },
            onWebViewLongClicked = {
                copyEntryUrl(context, checkNotNull(availableWebView))
            }.takeIf { availableWebView != null },
            onTrackingClicked = {
                if (!successState.hasLoggedInTrackers) {
                    navigator.push(SettingsScreen(SettingsScreen.Destination.Tracking))
                } else {
                    viewModel.showTrackDialog()
                }
            }.takeIf { viewModel.supportsTracking() },
            onDuplicatesClicked = viewModel::showDuplicateDialog.takeIf {
                successState.isFromSource &&
                    successState.entry.initialized &&
                    successState.duplicateCandidates.isNotEmpty()
            },
            onTagSearch = {
                scope.launch { performGenreSearch(navigator, entryCatalogueFeature, it, viewModel.source) }
            },
            onFilterButtonClicked = viewModel::showSettingsDialog,
            onRefresh = viewModel.sourceRefresh::fetchAll,
            onContinueReading = {
                scope.launch {
                    continueReading(context, viewModel, successState.entry)
                }
                Unit
            }.takeIf { viewModel.isContinueApplicable(successState.entry) },
            onSearch = { query, global -> scope.launch { performSearch(navigator, query, global) } },
            onCoverClicked = viewModel::showCoverDialog,
            onShareClicked = { shareEntry(context, checkNotNull(availableWebView)) }.takeIf {
                availableWebView != null
            },
            onDownloadActionClicked = viewModel::runDownloadAction
                .takeIf { bulkDownloadsAvailable },
            bookmarkedDownloadsSupported = bookmarkedDownloadsSupported,
            onEditCategoryClicked = viewModel::showChangeCategoryDialog.takeIf { successState.entry.favorite },
            onUpdatesClicked = { showUpdatesSheet = true }.takeIf { successState.entry.favorite },
            onEditDisplayNameClicked = viewModel::showEditDisplayNameDialog.takeIf { successState.entry.favorite },
            onManageMergeClicked = viewModel::showManageMergeSheet.takeIf { successState.isPartOfMerge },
            onOpenMergedEntryClicked = {
                navigator.push(EntryScreen(successState.mergeTargetId))
            }.takeIf { successState.showMergeNotice },
            onMigrateClicked = {
                viewModel.migrationSubject()?.let { navigator.push(MigrationConfigScreen(it)) }
                Unit
            }.takeIf { viewModel.migrationAvailable() },
            onRelatedEntriesClicked = {
                showRelatedEntriesDialog = true
            }.takeIf { relatedEntriesAvailability is EntryRelatedEntriesAvailability.Available },
            onEditNotesClicked = {
                navigator.push(eu.kanade.tachiyomi.ui.entry.notes.EntryNotesScreen(entry = successState.entry))
            },
            onMultiBookmarkClicked = viewModel::bookmarkChapters.takeIf { bookmarksSupported },
            onMultiMarkAsReadClicked = viewModel::markChaptersRead.takeIf { consumptionApplicable },
            onMarkPreviousAsReadClicked = viewModel::markPreviousChapterRead.takeIf { consumptionApplicable },
            onMultiDeleteClicked = viewModel::showDeleteChapterDialog,
            onChapterSwipe = viewModel::chapterSwipe,
            onMemberHeaderClicked = viewModel::toggleMemberExpanded,
            onGroupHeaderClicked = viewModel::toggleGroupExpanded,
            onGroupHeaderLongClicked = viewModel::toggleGroupSelection,
            onChapterSelected = viewModel::toggleSelection,
            onAllChapterSelected = viewModel::toggleAllSelection,
            onInvertSelection = viewModel::invertSelection,
            entryPreviewEnabled = viewModel.preview.isEnabled(successState.entry),
            entryPreviewSize = when (previewConfig.size) {
                EntryPreviewSize.SMALL -> PreviewSizeUi.SMALL
                EntryPreviewSize.MEDIUM -> PreviewSizeUi.MEDIUM
                EntryPreviewSize.LARGE -> PreviewSizeUi.LARGE
                EntryPreviewSize.EXTRA_LARGE -> PreviewSizeUi.EXTRA_LARGE
            },
            entryPreviewState = previewState,
            onPreviewExpandedChange = viewModel.preview::setExpanded,
            onPreviewRetry = viewModel.preview::retry,
            onPreviewPageLoad = viewModel.preview::loadPage,
            onPreviewPageClick = (
                { _: Long, pageIndex: Int ->
                    val target = viewModel.preview.openTarget(pageIndex)
                    if (target is EntryPreviewOpenTargetResult.Available) {
                        scope.launch {
                            openChapter(
                                context,
                                entryOpenFeature,
                                successState.entry,
                                successState.chapters.first {
                                    it.chapter.id == target.childId
                                }.chapter,
                                target.pageIndex,
                            )
                        }
                    }
                }
                ).takeIf { viewModel.preview.isOpenApplicable(successState.entry.type) },
            onOlderChaptersRetry = viewModel::retryOlderChapters,
        )

        translationSetup?.let { EntryTranslateSetupSheet(it, viewModel.translation) }

        if (showRelatedEntriesDialog) {
            RelatedEntriesDialog(
                viewModel = relatedEntriesViewModel,
                onDismissRequest = { showRelatedEntriesDialog = false },
                onEntryClick = { relatedEntry ->
                    showRelatedEntriesDialog = false
                    navigator.push(EntryScreen(relatedEntry.id, fromSource = true))
                },
            )
        }

        var showScanlatorsDialog by remember { mutableStateOf(false) }

        val onDismissRequest = { viewModel.dismissDialog() }
        when (val dialog = successState.dialog) {
            null -> {}
            is EntryViewModel.Dialog.ChangeCategory -> {
                ChangeCategoryDialog(
                    initialSelection = dialog.initialSelection,
                    onDismissRequest = onDismissRequest,
                    onEditCategories = { navigator.push(CategoryScreen()) },
                    onConfirm = { include, _ ->
                        viewModel.moveEntryToCategoriesAndAddToLibrary(dialog.entry, include)
                    },
                )
            }
            is EntryViewModel.Dialog.DeleteChapters -> {
                DeleteChaptersDialog(
                    entryType = successState.entry.type,
                    onDismissRequest = onDismissRequest,
                    onConfirm = {
                        viewModel.toggleAllSelection(false)
                        viewModel.deleteChapters(dialog.chapters)
                    },
                )
            }
            is EntryViewModel.Dialog.DownloadSettings -> {
                EntryDownloadSettingsDialog(
                    selectedCount = dialog.items.size,
                    choices = dialog.choices,
                    onSelect = viewModel::selectDownloadOption,
                    onDismissRequest = onDismissRequest,
                    onConfirm = viewModel::confirmDownloadSettings,
                )
            }
            is EntryViewModel.Dialog.DuplicateEntry -> {
                DuplicateEntrySheet(
                    duplicates = dialog.duplicates,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { viewModel.toggleFavorite(onRemoved = { _ -> }, checkDuplicate = false) },
                    onOpenEntry = { navigator.push(EntryScreen(it.id)) },
                    onMigrate = { viewModel.showMigrateDialog(it) },
                )
            }
            is EntryViewModel.Dialog.EditDisplayName -> {
                EditDisplayNameDialog(
                    initialValue = dialog.initialValue,
                    onDismissRequest = onDismissRequest,
                    onConfirm = viewModel::updateDisplayName,
                )
            }
            is EntryViewModel.Dialog.EditMerge -> {
                BrowseMergeEditorSheet(
                    entries = dialog.entries,
                    targetId = dialog.targetId,
                    targetLocked = dialog.targetLocked,
                    lock = dialog.lock,
                    removedIds = dialog.removedIds,
                    libraryRemovalIds = dialog.libraryRemovalIds,
                    confirmEnabled = dialog.enabled,
                    onDismissRequest = onDismissRequest,
                    onMove = viewModel::moveMergeEntry,
                    onSelectTarget = viewModel::setMergeTarget,
                    onToggleRemove = viewModel::toggleMergeEntryRemoval,
                    onToggleLibraryRemove = viewModel::toggleMergeEntryLibraryRemoval,
                    onConfirm = viewModel::confirmMerge,
                )
            }
            is EntryViewModel.Dialog.ManageMerge -> {
                ManageMergeSheet(
                    targetId = dialog.targetId,
                    targetLocked = dialog.targetLocked,
                    lock = dialog.lock,
                    members = dialog.members,
                    removableIds = dialog.removableIds,
                    libraryRemovalIds = dialog.libraryRemovalIds,
                    onDismissRequest = onDismissRequest,
                    onMove = viewModel::reorderMergeMembers,
                    onSaveOrder = viewModel::saveMergeOrder,
                    onOpenManga = { entryIdToOpen ->
                        viewModel.dismissDialog()
                        navigator.push(
                            EntryScreen(
                                entryIdToOpen,
                                bypassMerge = entryIdToOpen != dialog.savedTargetId,
                            ),
                        )
                    },
                    onSelectTarget = viewModel::setManageMergeTarget,
                    onToggleRemoveMember = viewModel::toggleMergedMemberRemoval,
                    onToggleRemoveMemberFromLibrary = viewModel::toggleMergedMemberLibraryRemoval,
                    onRemoveMembers = viewModel::removeMergedMembers,
                    onUnmergeAll = viewModel::unmergeAll,
                )
            }
            EntryViewModel.Dialog.RemoveFromLibrary -> {
                RemoveMangaDialog(
                    onDismissRequest = onDismissRequest,
                    onConfirm = viewModel::removeFromLibrary,
                    mangaToRemove = successState.entry,
                )
            }
            is EntryViewModel.Dialog.RemoveMergedEntry -> {
                DeleteLibraryEntriesDialog(
                    containsLocalEntries = dialog.containsLocalEntry,
                    containsMergedEntries = true,
                    onDismissRequest = onDismissRequest,
                    onConfirm = { deleteEntry, deleteChapter ->
                        viewModel.removeMergedEntry(dialog.members, deleteEntry, deleteChapter)
                    },
                )
            }
            is EntryViewModel.Dialog.Migrate -> {
                MigrateEntryDialog(
                    current = dialog.current,
                    target = dialog.target,
                    // Initiated from the context of [dialog.target] so we show [dialog.current].
                    onClickTitle = { navigator.push(EntryScreen(dialog.current.id)) },
                    onDismissRequest = onDismissRequest,
                )
            }
            is EntryViewModel.Dialog.SelectMergeTarget -> {
                MergeTargetPickerDialog(
                    title = context.stringResource(MR.strings.action_merge_into_library),
                    query = dialog.query,
                    visibleTargets = dialog.visibleTargets,
                    onDismissRequest = onDismissRequest,
                    onQueryChange = viewModel::updateMergeTargetQuery,
                    onSelectTarget = viewModel::openMergeEditor,
                )
            }
            EntryViewModel.Dialog.SettingsSheet -> EntryChapterSettingsDialog(
                onDismissRequest = onDismissRequest,
                entry = successState.entry,
                isMerged = successState.isMerged,
                onDownloadFilterChanged = viewModel::setDownloadedFilter,
                onUnreadFilterChanged = viewModel::setUnreadFilter,
                onBookmarkedFilterChanged = viewModel::setBookmarkedFilter,
                onFillerFilterChanged = viewModel::setFillerFilter.takeIf { successState.hasFiller },
                onSortModeChanged = viewModel::setSorting,
                onDisplayModeChanged = viewModel::setDisplayMode,
                onShowChapterPreviewsChanged = viewModel::setShowChapterPreviews
                    .takeIf { successState.hasChapterPreviews },
                onSetAsDefault = viewModel::setCurrentSettingsAsDefault,
                onResetToDefault = viewModel::resetToDefaultSettings,
                scanlatorFilterActive = successState.scanlatorFilterActive,
                onScanlatorFilterClicked = {
                    showScanlatorsDialog = true
                }.takeIf { successState.scanlatorFilterApplicable },
            )
            EntryViewModel.Dialog.TrackSheet -> {
                NavigatorAdaptiveSheet(
                    screen = TrackInfoDialogHomeScreen(
                        entry = successState.entry,
                    ),
                    enableSwipeDismiss = { it.lastItem is TrackInfoDialogHomeScreen },
                    onDismissRequest = onDismissRequest,
                )
            }
            EntryViewModel.Dialog.FullCover -> {
                val sm = assistedMetroViewModel<EntryCoverViewModel, EntryCoverViewModel.Factory> {
                    create(successState.entry.id)
                }
                val entry by sm.state.collectAsStateWithLifecycle()
                if (entry != null) {
                    val getContent = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
                        if (it == null) return@rememberLauncherForActivityResult
                        sm.editCover(context, it)
                    }
                    EntryCoverDialog(
                        coverData = entry!!,
                        artData = entry!!.backgroundUrl?.let { SourceImage(entry!!.source, it) },
                        snackbarHostState = sm.snackbarHostState,
                        isCustomCover = remember(entry) { sm.hasCustomCover(entry!!) },
                        isCoverEditable = entry!!.favorite || entry!!.isLocal(),
                        onShareClick = { sm.shareImage(context, it) },
                        onSaveClick = { sm.saveImage(context, it) },
                        onEditClick = {
                            when (it) {
                                EditCoverAction.EDIT -> getContent.launch("image/*")
                                EditCoverAction.DELETE -> sm.deleteCustomCover(context)
                                EditCoverAction.ADD_TO_LIBRARY -> sm.showAddToLibraryFirst(context)
                            }
                        },
                        onDismissRequest = onDismissRequest,
                    )
                } else {
                    LoadingScreen(Modifier.systemBarsPadding())
                }
            }
            is EntryViewModel.Dialog.SetFetchInterval -> {
                SetIntervalDialog(
                    interval = dialog.entry.fetchInterval,
                    nextUpdate = dialog.entry.expectedNextUpdate,
                    entryType = dialog.entry.type,
                    onDismissRequest = onDismissRequest,
                    onValueChanged = { interval: Int -> viewModel.setFetchInterval(dialog.entry, interval) }
                        .takeIf { viewModel.isUpdateIntervalEnabled },
                )
            }
        }

        val scanlatorFilterState = (
            successState.scanlatorFilterState as? EntryScanlatorFilterStateResult.Available
            )?.state
        if (showScanlatorsDialog && scanlatorFilterState != null) {
            ScanlatorFilterDialog(
                entryType = successState.entry.type,
                availableScanlators = scanlatorFilterState.availableScanlators,
                excludedScanlators = scanlatorFilterState.excludedScanlators,
                onDismissRequest = { showScanlatorsDialog = false },
                onConfirm = viewModel::setExcludedScanlators,
            )
        }
    }

    private suspend fun continueReading(context: Context, viewModel: EntryViewModel, entry: Entry) {
        viewModel.continueEntry(context, entry)
    }

    private suspend fun openChapter(
        context: Context,
        entryOpenFeature: EntryOpenFeature,
        entry: Entry,
        chapter: EntryChapter,
        pageIndex: Int? = null,
    ) {
        entryOpenFeature.open(
            context = context,
            entry = entry,
            chapter = chapter,
            options = EntryOpenOptions(pageIndex = pageIndex),
        )
    }

    private fun openEntryInWebView(
        navigator: Navigator,
        entry: Entry,
        webView: EntryWebViewResolution.Available,
    ) {
        navigator.push(
            WebViewScreen(
                url = webView.url,
                initialTitle = entry.title,
                sourceId = webView.sourceId,
                headers = webView.headers,
            ),
        )
    }

    private fun shareEntry(context: Context, webView: EntryWebViewResolution.Available) {
        try {
            val intent = webView.url.toUri().toShareIntent(context, type = "text/plain")
            context.startActivity(intent)
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
    private suspend fun performGenreSearch(
        navigator: Navigator,
        catalogueFeature: EntryCatalogueFeature,
        genreName: String,
        source: UnifiedSource?,
    ) {
        if (navigator.size < 2) {
            return
        }

        val previousController = navigator.items[navigator.size - 2]
        val hasCatalogue = source?.let {
            catalogueFeature.description(it.id).catalogue != null
        } == true
        if (previousController is CatalogScreen && hasCatalogue) {
            navigator.pop()
            previousController.searchGenre(genreName)
        } else {
            performSearch(navigator, genreName, global = false)
        }
    }

    /**
     * Copy Entry URL to Clipboard
     */
    private fun copyEntryUrl(context: Context, webView: EntryWebViewResolution.Available) {
        context.copyToClipboard(webView.url, webView.url)
    }
}

private fun LibraryPreferences.ChapterSwipeAction.availableFor(
    downloadsSupported: Boolean,
    bookmarksSupported: Boolean,
    consumptionSupported: Boolean,
): LibraryPreferences.ChapterSwipeAction {
    return when (this) {
        LibraryPreferences.ChapterSwipeAction.Download -> takeIf { downloadsSupported }
        LibraryPreferences.ChapterSwipeAction.ToggleBookmark -> takeIf { bookmarksSupported }
        LibraryPreferences.ChapterSwipeAction.ToggleRead -> takeIf { consumptionSupported }
        else -> this
    } ?: LibraryPreferences.ChapterSwipeAction.Disabled
}
