package eu.kanade.presentation.entry

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import eu.kanade.presentation.entry.activity.EntryActivityLine
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.components.ChapterGroupHeader
import eu.kanade.presentation.entry.components.EntryActionRow
import eu.kanade.presentation.entry.components.EntryBottomActionMenu
import eu.kanade.presentation.entry.components.EntryChapterHeader
import eu.kanade.presentation.entry.components.EntryChapterListItem
import eu.kanade.presentation.entry.components.EntryChapterSheet
import eu.kanade.presentation.entry.components.EntryContinueActions
import eu.kanade.presentation.entry.components.EntryInfoBox
import eu.kanade.presentation.entry.components.EntryToolbar
import eu.kanade.presentation.entry.components.ExpandableEntryDescription
import eu.kanade.presentation.entry.components.MergeNotice
import eu.kanade.presentation.entry.components.MergedMemberHeader
import eu.kanade.presentation.entry.components.MissingChapterCountListItem
import eu.kanade.presentation.entry.components.entryChapterDate
import eu.kanade.presentation.entry.components.entryChapterKindLabel
import eu.kanade.presentation.entry.components.entryChapterPreview
import eu.kanade.presentation.entry.components.entryChapterTitle
import eu.kanade.presentation.entry.components.olderChaptersItem
import eu.kanade.presentation.entry.components.preview.PreviewSizeUi
import eu.kanade.presentation.entry.translation.ChapterTranslateAction
import eu.kanade.presentation.entry.translation.EntryChapterTranslationUi
import eu.kanade.presentation.entry.updates.EntryUpdatesLabel
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.tachiyomi.ui.entry.EntryChapterList
import eu.kanade.tachiyomi.ui.entry.EntryViewModel
import eu.kanade.tachiyomi.ui.entry.activity.EntryActivityViewModel
import eu.kanade.tachiyomi.ui.entry.entrySelectionActionLabels
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.app.di.appGraph
import mihon.entry.interactions.child.EntryChildProgressLabel
import mihon.entry.interactions.download.EntryDownloadState
import mihon.entry.interactions.translate.EntryTranslateStatus
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.progress.PullRefresh
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Instant

@Composable
fun EntryScreen(
    state: EntryViewModel.State.Success,
    snackbarHostState: SnackbarHostState,
    updatesLabel: EntryUpdatesLabel,
    activity: EntryActivityViewModel.State,
    onActivityClicked: () -> Unit,
    isTabletUi: Boolean,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    navigateUp: () -> Unit,
    onChapterClicked: ((EntryChapter) -> Unit)?,
    onDownloadChapter: ((List<EntryChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    downloadProgressProvider: (chapterId: Long) -> Int,
    translation: EntryChapterTranslationUi?,
    onAddToLibraryClicked: () -> Unit,
    onAddToMergeClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,
    onDuplicatesClicked: (() -> Unit)?,

    // For tags menu
    onTagSearch: (String) -> Unit,

    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: (() -> Unit)?,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    bookmarkedDownloadsSupported: Boolean,
    onEditCategoryClicked: (() -> Unit)?,
    onUpdatesClicked: (() -> Unit)?,
    onEditDisplayNameClicked: (() -> Unit)?,
    onManageMergeClicked: (() -> Unit)?,
    onOpenMergedEntryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onMissingSourceClicked: (() -> Unit)?,
    onRelatedEntriesClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,

    // For bottom action menu
    onMultiBookmarkClicked: ((List<EntryChapter>, bookmarked: Boolean) -> Unit)?,
    onMultiMarkAsReadClicked: ((List<EntryChapter>, markAsRead: Boolean) -> Unit)?,
    onMarkPreviousAsReadClicked: ((EntryChapter) -> Unit)?,
    onMultiDeleteClicked: (List<EntryChapter>) -> Unit,

    // For chapter swipe
    onChapterSwipe: (EntryChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
    onMemberHeaderClicked: (Long) -> Unit,
    onGroupHeaderClicked: (ChapterGroupId) -> Unit,
    onGroupHeaderLongClicked: (ChapterGroupId) -> Unit,

    // Chapter selection
    onChapterSelected: (EntryChapterList.Item, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    entryPreviewEnabled: Boolean,
    entryPreviewSize: PreviewSizeUi,
    entryPreviewState: EntryViewModel.EntryPreviewState,
    onPreviewExpandedChange: (Boolean) -> Unit,
    onPreviewRetry: () -> Unit,
    onPreviewPageLoad: (Int) -> Unit,
    onPreviewPageClick: ((Long, Int) -> Unit)?,
    onOlderChaptersRetry: () -> Unit,
) {
    val context = LocalContext.current
    var previewSheetChapterId by rememberSaveable { mutableStateOf<Long?>(null) }
    val onChapterPreviewClicked: (EntryChapterList.Item) -> Unit = { previewSheetChapterId = it.id }
    previewSheetChapterId?.let { chapterId ->
        val item = state.processedChapters.find { it.id == chapterId }
        if (item == null) {
            previewSheetChapterId = null
        } else {
            EntryChapterSheet(
                entry = state.entry,
                item = item,
                onDismissRequest = { previewSheetChapterId = null },
                onOpen = onChapterClicked?.let { open -> { open(item.chapter) } },
            )
        }
    }
    val onCopyTagToClipboard: (tag: String) -> Unit = {
        if (it.isNotEmpty()) {
            context.copyToClipboard(it, it)
        }
    }

    if (!isTabletUi) {
        EntryScreenSmallImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            updatesLabel = updatesLabel,
            activity = activity,
            onActivityClicked = onActivityClicked,
            chapterSwipeStartAction = chapterSwipeStartAction,
            chapterSwipeEndAction = chapterSwipeEndAction,
            navigateUp = navigateUp,
            onChapterClicked = onChapterClicked,
            onDownloadChapter = onDownloadChapter,
            downloadProgressProvider = downloadProgressProvider,
            translation = translation,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onAddToMergeClicked = onAddToMergeClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onDuplicatesClicked = onDuplicatesClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
            onFilterClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueReading = onContinueReading,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            bookmarkedDownloadsSupported = bookmarkedDownloadsSupported,
            onEditCategoryClicked = onEditCategoryClicked,
            onUpdatesClicked = onUpdatesClicked,
            onEditDisplayNameClicked = onEditDisplayNameClicked,
            onManageMergeClicked = onManageMergeClicked,
            onOpenMergedEntryClicked = onOpenMergedEntryClicked,
            onMigrateClicked = onMigrateClicked,
            onMissingSourceClicked = onMissingSourceClicked,
            onRelatedEntriesClicked = onRelatedEntriesClicked,
            onEditNotesClicked = onEditNotesClicked,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
            onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onChapterSwipe = onChapterSwipe,
            onMemberHeaderClicked = onMemberHeaderClicked,
            onGroupHeaderClicked = onGroupHeaderClicked,
            onGroupHeaderLongClicked = onGroupHeaderLongClicked,
            onChapterPreviewClicked = onChapterPreviewClicked,
            onChapterSelected = onChapterSelected,
            onAllChapterSelected = onAllChapterSelected,
            onInvertSelection = onInvertSelection,
            entryPreviewEnabled = entryPreviewEnabled,
            entryPreviewSize = entryPreviewSize,
            entryPreviewState = entryPreviewState,
            onPreviewExpandedChange = onPreviewExpandedChange,
            onPreviewRetry = onPreviewRetry,
            onPreviewPageLoad = onPreviewPageLoad,
            onPreviewPageClick = onPreviewPageClick,
            onOlderChaptersRetry = onOlderChaptersRetry,
        )
    } else {
        EntryScreenLargeImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            chapterSwipeStartAction = chapterSwipeStartAction,
            chapterSwipeEndAction = chapterSwipeEndAction,
            updatesLabel = updatesLabel,
            activity = activity,
            onActivityClicked = onActivityClicked,
            navigateUp = navigateUp,
            onChapterClicked = onChapterClicked,
            onDownloadChapter = onDownloadChapter,
            downloadProgressProvider = downloadProgressProvider,
            translation = translation,
            onAddToLibraryClicked = onAddToLibraryClicked,
            onAddToMergeClicked = onAddToMergeClicked,
            onWebViewClicked = onWebViewClicked,
            onWebViewLongClicked = onWebViewLongClicked,
            onTrackingClicked = onTrackingClicked,
            onDuplicatesClicked = onDuplicatesClicked,
            onTagSearch = onTagSearch,
            onCopyTagToClipboard = onCopyTagToClipboard,
            onFilterButtonClicked = onFilterButtonClicked,
            onRefresh = onRefresh,
            onContinueReading = onContinueReading,
            onSearch = onSearch,
            onCoverClicked = onCoverClicked,
            onShareClicked = onShareClicked,
            onDownloadActionClicked = onDownloadActionClicked,
            bookmarkedDownloadsSupported = bookmarkedDownloadsSupported,
            onEditCategoryClicked = onEditCategoryClicked,
            onUpdatesClicked = onUpdatesClicked,
            onEditDisplayNameClicked = onEditDisplayNameClicked,
            onManageMergeClicked = onManageMergeClicked,
            onOpenMergedEntryClicked = onOpenMergedEntryClicked,
            onMigrateClicked = onMigrateClicked,
            onMissingSourceClicked = onMissingSourceClicked,
            onRelatedEntriesClicked = onRelatedEntriesClicked,
            onEditNotesClicked = onEditNotesClicked,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
            onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onChapterSwipe = onChapterSwipe,
            onMemberHeaderClicked = onMemberHeaderClicked,
            onGroupHeaderClicked = onGroupHeaderClicked,
            onGroupHeaderLongClicked = onGroupHeaderLongClicked,
            onChapterPreviewClicked = onChapterPreviewClicked,
            onChapterSelected = onChapterSelected,
            onAllChapterSelected = onAllChapterSelected,
            onInvertSelection = onInvertSelection,
            entryPreviewEnabled = entryPreviewEnabled,
            entryPreviewSize = entryPreviewSize,
            entryPreviewState = entryPreviewState,
            onPreviewExpandedChange = onPreviewExpandedChange,
            onPreviewRetry = onPreviewRetry,
            onPreviewPageLoad = onPreviewPageLoad,
            onPreviewPageClick = onPreviewPageClick,
            onOlderChaptersRetry = onOlderChaptersRetry,
        )
    }
}

@Composable
private fun EntryScreenSmallImpl(
    state: EntryViewModel.State.Success,
    snackbarHostState: SnackbarHostState,
    updatesLabel: EntryUpdatesLabel,
    activity: EntryActivityViewModel.State,
    onActivityClicked: () -> Unit,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    navigateUp: () -> Unit,
    onChapterClicked: ((EntryChapter) -> Unit)?,
    onDownloadChapter: ((List<EntryChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    downloadProgressProvider: (chapterId: Long) -> Int,
    translation: EntryChapterTranslationUi?,
    onAddToLibraryClicked: () -> Unit,
    onAddToMergeClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,
    onDuplicatesClicked: (() -> Unit)?,

    // For tags menu
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,

    onFilterClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: (() -> Unit)?,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    bookmarkedDownloadsSupported: Boolean,
    onEditCategoryClicked: (() -> Unit)?,
    onUpdatesClicked: (() -> Unit)?,
    onEditDisplayNameClicked: (() -> Unit)?,
    onManageMergeClicked: (() -> Unit)?,
    onOpenMergedEntryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onMissingSourceClicked: (() -> Unit)?,
    onRelatedEntriesClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,

    // For bottom action menu
    onMultiBookmarkClicked: ((List<EntryChapter>, bookmarked: Boolean) -> Unit)?,
    onMultiMarkAsReadClicked: ((List<EntryChapter>, markAsRead: Boolean) -> Unit)?,
    onMarkPreviousAsReadClicked: ((EntryChapter) -> Unit)?,
    onMultiDeleteClicked: (List<EntryChapter>) -> Unit,

    // For chapter swipe
    onChapterSwipe: (EntryChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
    onMemberHeaderClicked: (Long) -> Unit,
    onGroupHeaderClicked: (ChapterGroupId) -> Unit,
    onGroupHeaderLongClicked: (ChapterGroupId) -> Unit,
    onChapterPreviewClicked: (EntryChapterList.Item) -> Unit,

    // Chapter selection
    onChapterSelected: (EntryChapterList.Item, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    entryPreviewEnabled: Boolean,
    entryPreviewSize: PreviewSizeUi,
    entryPreviewState: EntryViewModel.EntryPreviewState,
    onPreviewExpandedChange: (Boolean) -> Unit,
    onPreviewRetry: () -> Unit,
    onPreviewPageLoad: (Int) -> Unit,
    onPreviewPageClick: ((Long, Int) -> Unit)?,
    onOlderChaptersRetry: () -> Unit,
) {
    val chapterListState = rememberLazyListState()
    var topBarHeight by remember { mutableIntStateOf(0) }
    var highlightedChapterId by remember { mutableStateOf<Long?>(null) }
    var chapterHighlightGeneration by remember { mutableIntStateOf(0) }

    val (chapters, listItem, isAnySelected) = remember(state) {
        Triple(
            first = state.processedChapters,
            second = state.chapterListItems,
            third = state.isAnySelected,
        )
    }

    BackHandler(enabled = isAnySelected) {
        onAllChapterSelected(false)
    }

    Scaffold(
        topBar = {
            val selectedChapterCount = state.selectedChapters.size
            val isFirstItemVisible by remember {
                derivedStateOf { chapterListState.firstVisibleItemIndex == 0 }
            }
            val isFirstItemScrolled by remember {
                derivedStateOf { chapterListState.firstVisibleItemScrollOffset > 0 }
            }
            val titleAlpha by animateFloatAsState(
                if (!isFirstItemVisible) 1f else 0f,
                label = "Top Bar Title",
            )
            val backgroundAlpha by animateFloatAsState(
                if (!isFirstItemVisible || isFirstItemScrolled) 1f else 0f,
                label = "Top Bar Background",
            )
            EntryToolbar(
                modifier = Modifier.onSizeChanged { topBarHeight = it.height },
                title = state.entry.displayTitle,
                hasFilters = state.filterActive,
                navigateUp = navigateUp,
                onClickFilter = onFilterClicked,
                onClickShare = onShareClicked,
                onClickDownload = onDownloadActionClicked,
                bookmarkedDownloadsSupported = bookmarkedDownloadsSupported,
                onClickEditCategory = onEditCategoryClicked,
                onClickEditDisplayName = onEditDisplayNameClicked,
                onClickManageMerge = onManageMergeClicked,
                onClickRefresh = onRefresh,
                onClickMigrate = onMigrateClicked,
                onClickRelatedEntries = onRelatedEntriesClicked,
                onClickEditNotes = onEditNotesClicked,
                entryType = state.entry.type,
                actionModeCounter = selectedChapterCount,
                onCancelActionMode = { onAllChapterSelected(false) },
                onSelectAll = { onAllChapterSelected(true) },
                onInvertSelection = { onInvertSelection() },
                titleAlphaProvider = { titleAlpha },
                backgroundAlphaProvider = { backgroundAlpha },
            )
        },
        bottomBar = {
            SharedEntryBottomActionMenu(
                selected = state.selectedChapters,
                childProgressLabels = state.childProgressLabels,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
                onDownloadChapter = onDownloadChapter,
                translation = translation,
                onMultiDeleteClicked = onMultiDeleteClicked,
                fillFraction = 1f,
            )
        },
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            onContinueReading?.let { continueAction ->
                // Shown once progress is known, so it doesn't offer to start what is already being read.
                val isFABVisible = state.hasUnreadChapters && !isAnySelected && state.childProgressLoaded
                val targetListIndex = state.chapterListIndex(state.continueTargetChapterId)
                    ?.plus(if (state.showMergeNotice) 5 else 4)
                EntryContinueActions(
                    listState = chapterListState,
                    continueTargetListIndex = targetListIndex,
                    topOcclusionPx = topBarHeight,
                    isReading = state.hasStartedReading,
                    targetChapter = state.continueTargetChapter,
                    entryType = state.entry.type,
                    visible = isFABVisible,
                    onTargetReached = {
                        highlightedChapterId = state.continueTargetChapterId
                        chapterHighlightGeneration++
                    },
                    onContinueReading = continueAction,
                )
            }
        },
    ) { contentPadding ->
        val topPadding = contentPadding.calculateTopPadding()

        PullRefresh(
            refreshing = state.isRefreshingData,
            onRefresh = onRefresh,
            enabled = !isAnySelected,
            indicatorPadding = PaddingValues(top = topPadding),
        ) {
            val layoutDirection = LocalLayoutDirection.current
            VerticalFastScroller(
                listState = chapterListState,
                topContentPadding = topPadding,
                endContentPadding = contentPadding.calculateEndPadding(layoutDirection),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    state = chapterListState,
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        bottom = contentPadding.calculateBottomPadding(),
                    ),
                ) {
                    item(
                        key = EntryScreenItem.INFO_BOX,
                        contentType = EntryScreenItem.INFO_BOX,
                    ) {
                        EntryInfoBox(
                            isTabletUi = false,
                            appBarPadding = topPadding,
                            entry = state.entry,
                            sourceName = state.sourceName,
                            isStubSource = state.isSourceMissing,
                            mergedEntryCount = state.memberIds.size,
                            onCoverClick = onCoverClicked,
                            onSourceClick = sourceClickAction(
                                state,
                                onManageMergeClicked,
                                onMissingSourceClicked,
                                onSearch,
                            ),
                            onMergedEntriesClick = onManageMergeClicked,
                            doSearch = onSearch,
                        )
                    }

                    if (state.showMergeNotice) {
                        item(
                            key = EntryScreenItem.MERGE_NOTICE,
                            contentType = EntryScreenItem.MERGE_NOTICE,
                        ) {
                            MergeNotice(
                                onOpenMergedEntry = onOpenMergedEntryClicked,
                                onManageMerge = onManageMergeClicked,
                            )
                        }
                    }

                    item(
                        key = EntryScreenItem.ACTION_ROW,
                        contentType = EntryScreenItem.ACTION_ROW,
                    ) {
                        // The activity line shares the action row's item so the continue target's list index, which
                        // counts the items above the chapters, stays the same whether or not the line is shown.
                        Column {
                            EntryActionRow(
                                favorite = state.entry.favorite,
                                trackingCount = state.trackingCount,
                                updatesLabel = updatesLabel,
                                onAddToLibraryClicked = onAddToLibraryClicked,
                                onAddToMergeClicked = onAddToMergeClicked,
                                onWebViewClicked = onWebViewClicked,
                                onWebViewLongClicked = onWebViewLongClicked,
                                onTrackingClicked = onTrackingClicked,
                                onDuplicatesClicked = onDuplicatesClicked,
                                onUpdatesClicked = onUpdatesClicked,
                                onEditCategory = onEditCategoryClicked,
                            )
                            activity.summary?.let { EntryActivityLine(summary = it, onClick = onActivityClicked) }
                        }
                    }

                    item(
                        key = EntryScreenItem.DESCRIPTION_WITH_TAG,
                        contentType = EntryScreenItem.DESCRIPTION_WITH_TAG,
                    ) {
                        ExpandableEntryDescription(
                            defaultExpandState = false,
                            description = state.entry.description,
                            tagsProvider = { state.entry.genre },
                            notes = state.entry.notes,
                            onTagSearch = onTagSearch,
                            onCopyTagToClipboard = onCopyTagToClipboard,
                            onEditNotes = onEditNotesClicked,
                            entryPreviewEnabled = entryPreviewEnabled,
                            entryPreviewSize = entryPreviewSize,
                            entryPreviewState = entryPreviewState,
                            onPreviewExpandedChange = onPreviewExpandedChange,
                            onPreviewRetry = onPreviewRetry,
                            onPreviewPageLoad = onPreviewPageLoad,
                            onPreviewPageClick = onPreviewPageClick,
                        )
                    }

                    if (state.childListApplicable) {
                        item(
                            key = EntryScreenItem.CHAPTER_HEADER,
                            contentType = EntryScreenItem.CHAPTER_HEADER,
                        ) {
                            EntryChapterHeader(
                                enabled = !isAnySelected,
                                entryType = state.entry.type,
                                chapterCount = chapters.size,
                                chapterCountSoFar = state.olderChapters != null,
                                missingChapterCount = state.missingChildCount,
                                unreadCount = state.chapterProgress.unreadCount,
                                catchUpMillis = activity.paceMillis?.let { it * state.chapterProgress.unreadCount },
                                onClick = onFilterClicked,
                            )
                        }

                        // Older chapters arrive at the oldest end of the list
                        val olderChaptersFirst = !state.entry.sortDescending()
                        if (olderChaptersFirst) {
                            olderChaptersItem(
                                state.olderChapters,
                                beforeChapters = true,
                                onOlderChaptersRetry,
                            )
                        }

                        sharedChapterItems(
                            entry = state.entry,
                            chapters = listItem,
                            childProgressLabels = state.childProgressLabels,
                            readChapterDurations = activity.readChapterDurations,
                            highlightedChapterId = highlightedChapterId,
                            chapterHighlightGeneration = chapterHighlightGeneration,
                            isAnyChapterSelected = isAnySelected,
                            chapterSwipeStartAction = chapterSwipeStartAction,
                            chapterSwipeEndAction = chapterSwipeEndAction,
                            onChapterClicked = onChapterClicked,
                            onDownloadChapter = onDownloadChapter,
                            downloadProgressProvider = downloadProgressProvider,
                            translation = translation,
                            onChapterSelected = onChapterSelected,
                            onChapterSwipe = onChapterSwipe,
                            onMemberHeaderClicked = onMemberHeaderClicked,
                            onGroupHeaderClicked = onGroupHeaderClicked,
                            onGroupHeaderLongClicked = onGroupHeaderLongClicked,
                            onChapterPreviewClicked = onChapterPreviewClicked,
                        )
                        if (!olderChaptersFirst) {
                            olderChaptersItem(
                                state.olderChapters,
                                beforeChapters = false,
                                onOlderChaptersRetry,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EntryScreenLargeImpl(
    state: EntryViewModel.State.Success,
    snackbarHostState: SnackbarHostState,
    updatesLabel: EntryUpdatesLabel,
    activity: EntryActivityViewModel.State,
    onActivityClicked: () -> Unit,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    navigateUp: () -> Unit,
    onChapterClicked: ((EntryChapter) -> Unit)?,
    onDownloadChapter: ((List<EntryChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    downloadProgressProvider: (chapterId: Long) -> Int,
    translation: EntryChapterTranslationUi?,
    onAddToLibraryClicked: () -> Unit,
    onAddToMergeClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: (() -> Unit)?,
    onDuplicatesClicked: (() -> Unit)?,

    // For tags menu
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,

    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: (() -> Unit)?,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    bookmarkedDownloadsSupported: Boolean,
    onEditCategoryClicked: (() -> Unit)?,
    onUpdatesClicked: (() -> Unit)?,
    onEditDisplayNameClicked: (() -> Unit)?,
    onManageMergeClicked: (() -> Unit)?,
    onOpenMergedEntryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onMissingSourceClicked: (() -> Unit)?,
    onRelatedEntriesClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,

    // For bottom action menu
    onMultiBookmarkClicked: ((List<EntryChapter>, bookmarked: Boolean) -> Unit)?,
    onMultiMarkAsReadClicked: ((List<EntryChapter>, markAsRead: Boolean) -> Unit)?,
    onMarkPreviousAsReadClicked: ((EntryChapter) -> Unit)?,
    onMultiDeleteClicked: (List<EntryChapter>) -> Unit,

    // For swipe actions
    onChapterSwipe: (EntryChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
    onMemberHeaderClicked: (Long) -> Unit,
    onGroupHeaderClicked: (ChapterGroupId) -> Unit,
    onGroupHeaderLongClicked: (ChapterGroupId) -> Unit,
    onChapterPreviewClicked: (EntryChapterList.Item) -> Unit,

    // Chapter selection
    onChapterSelected: (EntryChapterList.Item, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    entryPreviewEnabled: Boolean,
    entryPreviewSize: PreviewSizeUi,
    entryPreviewState: EntryViewModel.EntryPreviewState,
    onPreviewExpandedChange: (Boolean) -> Unit,
    onPreviewRetry: () -> Unit,
    onPreviewPageLoad: (Int) -> Unit,
    onPreviewPageClick: ((Long, Int) -> Unit)?,
    onOlderChaptersRetry: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    val (chapters, listItem, isAnySelected) = remember(state) {
        Triple(
            first = state.processedChapters,
            second = state.chapterListItems,
            third = state.isAnySelected,
        )
    }

    val insetPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues()
    var topBarHeight by remember { mutableIntStateOf(0) }

    val chapterListState = rememberLazyListState()
    var highlightedChapterId by remember { mutableStateOf<Long?>(null) }
    var chapterHighlightGeneration by remember { mutableIntStateOf(0) }

    BackHandler(enabled = isAnySelected) {
        onAllChapterSelected(false)
    }

    Scaffold(
        topBar = {
            val selectedChapterCount = state.selectedChapters.size
            EntryToolbar(
                modifier = Modifier.onSizeChanged { topBarHeight = it.height },
                title = state.entry.displayTitle,
                hasFilters = state.filterActive,
                navigateUp = navigateUp,
                onClickFilter = onFilterButtonClicked,
                onClickShare = onShareClicked,
                onClickDownload = onDownloadActionClicked,
                bookmarkedDownloadsSupported = bookmarkedDownloadsSupported,
                onClickEditCategory = onEditCategoryClicked,
                onClickEditDisplayName = onEditDisplayNameClicked,
                onClickManageMerge = onManageMergeClicked,
                onClickRefresh = onRefresh,
                onClickMigrate = onMigrateClicked,
                onClickRelatedEntries = onRelatedEntriesClicked,
                onClickEditNotes = onEditNotesClicked,
                entryType = state.entry.type,
                onCancelActionMode = { onAllChapterSelected(false) },
                actionModeCounter = selectedChapterCount,
                onSelectAll = { onAllChapterSelected(true) },
                onInvertSelection = { onInvertSelection() },
                titleAlphaProvider = { 1f },
                backgroundAlphaProvider = { 1f },
            )
        },
        bottomBar = {
            // Aligned by its own modifier: a wrapper would always be laid out, and the scaffold then drops the
            // navigation bar inset under the FAB
            SharedEntryBottomActionMenu(
                selected = state.selectedChapters,
                childProgressLabels = state.childProgressLabels,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
                onDownloadChapter = onDownloadChapter,
                translation = translation,
                onMultiDeleteClicked = onMultiDeleteClicked,
                fillFraction = 0.5f,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(Alignment.End),
            )
        },
        snackbarHostState = snackbarHostState,
        floatingActionButton = {
            onContinueReading?.let { continueAction ->
                // Shown once progress is known, so it doesn't offer to start what is already being read.
                val isFABVisible = state.hasUnreadChapters && !isAnySelected && state.childProgressLoaded
                val targetListIndex = state.chapterListIndex(state.continueTargetChapterId)?.plus(1)
                EntryContinueActions(
                    listState = chapterListState,
                    continueTargetListIndex = targetListIndex,
                    topOcclusionPx = topBarHeight,
                    isReading = state.hasStartedReading,
                    targetChapter = state.continueTargetChapter,
                    entryType = state.entry.type,
                    visible = isFABVisible,
                    onTargetReached = {
                        highlightedChapterId = state.continueTargetChapterId
                        chapterHighlightGeneration++
                    },
                    onContinueReading = continueAction,
                )
            }
        },
    ) { contentPadding ->
        PullRefresh(
            refreshing = state.isRefreshingData,
            onRefresh = onRefresh,
            enabled = !isAnySelected,
            indicatorPadding = PaddingValues(
                start = insetPadding.calculateStartPadding(layoutDirection),
                top = with(density) { topBarHeight.toDp() },
                end = insetPadding.calculateEndPadding(layoutDirection),
            ),
        ) {
            TwoPanelBox(
                modifier = Modifier.padding(
                    start = contentPadding.calculateStartPadding(layoutDirection),
                    end = contentPadding.calculateEndPadding(layoutDirection),
                ),
                startContent = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = contentPadding.calculateBottomPadding()),
                    ) {
                        EntryInfoBox(
                            isTabletUi = true,
                            appBarPadding = contentPadding.calculateTopPadding(),
                            entry = state.entry,
                            sourceName = state.sourceName,
                            isStubSource = state.isSourceMissing,
                            mergedEntryCount = state.memberIds.size,
                            onCoverClick = onCoverClicked,
                            onSourceClick = sourceClickAction(
                                state,
                                onManageMergeClicked,
                                onMissingSourceClicked,
                                onSearch,
                            ),
                            onMergedEntriesClick = onManageMergeClicked,
                            doSearch = onSearch,
                        )
                        if (state.showMergeNotice) {
                            MergeNotice(
                                onOpenMergedEntry = onOpenMergedEntryClicked,
                                onManageMerge = onManageMergeClicked,
                            )
                        }
                        EntryActionRow(
                            favorite = state.entry.favorite,
                            trackingCount = state.trackingCount,
                            updatesLabel = updatesLabel,
                            onAddToLibraryClicked = onAddToLibraryClicked,
                            onAddToMergeClicked = onAddToMergeClicked,
                            onWebViewClicked = onWebViewClicked,
                            onWebViewLongClicked = onWebViewLongClicked,
                            onTrackingClicked = onTrackingClicked,
                            onDuplicatesClicked = onDuplicatesClicked,
                            onUpdatesClicked = onUpdatesClicked,
                            onEditCategory = onEditCategoryClicked,
                        )
                        activity.summary?.let { EntryActivityLine(summary = it, onClick = onActivityClicked) }
                        ExpandableEntryDescription(
                            defaultExpandState = false,
                            description = state.entry.description,
                            tagsProvider = { state.entry.genre },
                            notes = state.entry.notes,
                            onTagSearch = onTagSearch,
                            onCopyTagToClipboard = onCopyTagToClipboard,
                            onEditNotes = onEditNotesClicked,
                            entryPreviewEnabled = entryPreviewEnabled,
                            entryPreviewSize = entryPreviewSize,
                            entryPreviewState = entryPreviewState,
                            onPreviewExpandedChange = onPreviewExpandedChange,
                            onPreviewRetry = onPreviewRetry,
                            onPreviewPageLoad = onPreviewPageLoad,
                            onPreviewPageClick = onPreviewPageClick,
                        )
                    }
                },
                endContent = {
                    VerticalFastScroller(
                        listState = chapterListState,
                        topContentPadding = contentPadding.calculateTopPadding(),
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxHeight(),
                            state = chapterListState,
                            contentPadding = PaddingValues(
                                top = contentPadding.calculateTopPadding(),
                                bottom = contentPadding.calculateBottomPadding(),
                            ),
                        ) {
                            if (state.childListApplicable) {
                                item(
                                    key = EntryScreenItem.CHAPTER_HEADER,
                                    contentType = EntryScreenItem.CHAPTER_HEADER,
                                ) {
                                    EntryChapterHeader(
                                        enabled = !isAnySelected,
                                        entryType = state.entry.type,
                                        chapterCount = chapters.size,
                                        chapterCountSoFar = state.olderChapters != null,
                                        missingChapterCount = state.missingChildCount,
                                        unreadCount = state.chapterProgress.unreadCount,
                                        catchUpMillis = activity.paceMillis?.let {
                                            it *
                                                state.chapterProgress.unreadCount
                                        },
                                        onClick = onFilterButtonClicked,
                                    )
                                }

                                // Older chapters arrive at the oldest end of the list
                                val olderChaptersFirst = !state.entry.sortDescending()
                                if (olderChaptersFirst) {
                                    olderChaptersItem(
                                        state.olderChapters,
                                        beforeChapters = true,
                                        onOlderChaptersRetry,
                                    )
                                }

                                sharedChapterItems(
                                    entry = state.entry,
                                    chapters = listItem,
                                    childProgressLabels = state.childProgressLabels,
                                    readChapterDurations = activity.readChapterDurations,
                                    highlightedChapterId = highlightedChapterId,
                                    chapterHighlightGeneration = chapterHighlightGeneration,
                                    isAnyChapterSelected = isAnySelected,
                                    chapterSwipeStartAction = chapterSwipeStartAction,
                                    chapterSwipeEndAction = chapterSwipeEndAction,
                                    onChapterClicked = onChapterClicked,
                                    onDownloadChapter = onDownloadChapter,
                                    downloadProgressProvider = downloadProgressProvider,
                                    translation = translation,
                                    onChapterSelected = onChapterSelected,
                                    onChapterSwipe = onChapterSwipe,
                                    onMemberHeaderClicked = onMemberHeaderClicked,
                                    onGroupHeaderClicked = onGroupHeaderClicked,
                                    onGroupHeaderLongClicked = onGroupHeaderLongClicked,
                                    onChapterPreviewClicked = onChapterPreviewClicked,
                                )
                                if (!olderChaptersFirst) {
                                    olderChaptersItem(
                                        state.olderChapters,
                                        beforeChapters = false,
                                        onOlderChaptersRetry,
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SharedEntryBottomActionMenu(
    selected: List<EntryChapterList.Item>,
    childProgressLabels: Map<Long, EntryChildProgressLabel>,
    onMultiBookmarkClicked: ((List<EntryChapter>, bookmarked: Boolean) -> Unit)?,
    onMultiMarkAsReadClicked: ((List<EntryChapter>, markAsRead: Boolean) -> Unit)?,
    onMarkPreviousAsReadClicked: ((EntryChapter) -> Unit)?,
    onDownloadChapter: ((List<EntryChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    translation: EntryChapterTranslationUi?,
    onMultiDeleteClicked: (List<EntryChapter>) -> Unit,
    fillFraction: Float,
    modifier: Modifier = Modifier,
) {
    val presentationFeature = LocalContext.current.appGraph.entryTypePresentationFeature
    val selectionTypes = remember(selected) { selected.fastMap { it.entry.type } }
    val selectionPresentation = remember(selectionTypes) {
        selectionTypes.selectionEntryTypePresentation(presentationFeature)
    }
    val selectionActionLabels = remember(selectionTypes) {
        selectionTypes.entrySelectionActionLabels(presentationFeature)
    }
    EntryBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = modifier.fillMaxWidth(fillFraction),
        onBookmarkClicked = onMultiBookmarkClicked?.let { callback ->
            { callback(selected.fastMap { it.chapter }, true) }
        }?.takeIf { selected.fastAny { !it.chapter.bookmark } },
        onRemoveBookmarkClicked = onMultiBookmarkClicked?.let { callback ->
            { callback(selected.fastMap { it.chapter }, false) }
        }?.takeIf { selected.fastAll { it.chapter.bookmark } },
        bookmarkLabel = selectionPresentation.bookmarkChildLabel,
        removeBookmarkLabel = selectionPresentation.removeBookmarkChildLabel,
        onMarkAsReadClicked = onMultiMarkAsReadClicked?.let { callback ->
            { callback(selected.fastMap { it.chapter }, true) }
        }?.takeIf { selected.fastAny { !it.chapter.read } },
        onMarkAsUnreadClicked = onMultiMarkAsReadClicked?.let { callback ->
            { callback(selected.fastMap { it.chapter }, false) }
        }?.takeIf { selected.fastAny { it.chapter.read || it.chapter.id in childProgressLabels } },
        markAsReadLabel = selectionActionLabels.markAsReadLabel,
        markAsUnreadLabel = selectionActionLabels.markAsUnreadLabel,
        markPreviousAsReadLabel = selectionPresentation.markPreviousAsConsumedLabel,
        downloadPresentation = selectionPresentation,
        onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked?.let { callback ->
            { callback(selected[0].chapter) }
        }?.takeIf { selected.size == 1 },
        onDownloadClicked = {
            onDownloadChapter!!(selected.toList(), ChapterDownloadAction.START)
        }.takeIf {
            onDownloadChapter != null && selected.fastAny { it.downloadState != EntryDownloadState.DOWNLOADED }
        },
        onDeleteClicked = {
            onMultiDeleteClicked(selected.fastMap { it.chapter })
        }.takeIf {
            selected.fastAny { it.downloadState == EntryDownloadState.DOWNLOADED }
        },
        onTranslateClicked = translation?.let { ui ->
            { ui.onAction(selected.toList(), ChapterTranslateAction.TRANSLATE) }
        }?.takeIf {
            selected.fastAny { item ->
                val status = translation.statuses[item.id]
                status == null || status is EntryTranslateStatus.Failed
            }
        },
        onDeleteTranslationClicked = translation?.let { ui ->
            { ui.onAction(selected.toList(), ChapterTranslateAction.DELETE_TRANSLATION) }
        }?.takeIf {
            selected.fastAny { translation.statuses[it.id] == EntryTranslateStatus.Translated }
        },
    )
}

private fun LazyListScope.sharedChapterItems(
    entry: Entry,
    chapters: List<EntryChapterList>,
    childProgressLabels: Map<Long, EntryChildProgressLabel>,
    readChapterDurations: Map<Long, Long>,
    highlightedChapterId: Long?,
    chapterHighlightGeneration: Int,
    isAnyChapterSelected: Boolean,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onChapterClicked: ((EntryChapter) -> Unit)?,
    onDownloadChapter: ((List<EntryChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    downloadProgressProvider: (chapterId: Long) -> Int,
    translation: EntryChapterTranslationUi?,
    onChapterSelected: (EntryChapterList.Item, Boolean, Boolean) -> Unit,
    onChapterSwipe: (EntryChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
    onMemberHeaderClicked: (Long) -> Unit,
    onGroupHeaderClicked: (ChapterGroupId) -> Unit,
    onGroupHeaderLongClicked: (ChapterGroupId) -> Unit,
    onChapterPreviewClicked: (EntryChapterList.Item) -> Unit,
) {
    // Member headers, and group headers outside a merge, split the list, so each member's or group's chapters are a
    // card of their own; a merged member's group headers are rows of its card.
    val positions = RowGroupPosition.ofRuns(chapters) {
        it !is EntryChapterList.MemberHeader && (it !is EntryChapterList.GroupHeader || it.nested)
    }
    itemsIndexed(
        items = chapters,
        key = { _, item ->
            when (item) {
                is EntryChapterList.MemberHeader -> "member-header-${item.entryId}"
                is EntryChapterList.GroupHeader ->
                    "group-header-${item.group.entryId}-${item.group.groupKey?.let { "key-$it" } ?: "none"}"
                is EntryChapterList.MissingCount -> "missing-count-${item.id}"
                is EntryChapterList.Item -> "chapter-${item.id}"
            }
        },
        contentType = { _, item ->
            when (item) {
                is EntryChapterList.MemberHeader,
                is EntryChapterList.GroupHeader,
                -> EntryScreenItem.CHAPTER_GROUP_HEADER
                else -> EntryScreenItem.CHAPTER
            }
        },
    ) { index, item ->
        val haptic = LocalHapticFeedback.current
        val formatDuration = rememberStatisticsDurationFormatter()

        when (item) {
            is EntryChapterList.MemberHeader -> {
                MergedMemberHeader(
                    title = item.title,
                    sourceName = item.sourceName,
                    childCount = item.childCount,
                    unreadCount = item.unreadCount,
                    expanded = item.expanded,
                    entryType = entry.type,
                    onClick = { onMemberHeaderClicked(item.entryId) },
                )
            }
            is EntryChapterList.GroupHeader -> {
                val header = @Composable {
                    ChapterGroupHeader(
                        title = item.title,
                        childCount = item.childCount,
                        unreadCount = item.unreadCount,
                        expanded = item.expanded,
                        nested = item.nested,
                        entryType = entry.type,
                        onClick = { onGroupHeaderClicked(item.group) },
                        onLongClick = {
                            onGroupHeaderLongClicked(item.group)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    )
                }
                if (item.nested) {
                    RowGroupItem(position = positions[index] ?: RowGroupPosition.Alone, content = header)
                } else {
                    header()
                }
            }
            is EntryChapterList.MissingCount -> RowGroupItem(position = positions[index] ?: RowGroupPosition.Alone) {
                MissingChapterCountListItem(count = item.count, entryType = entry.type)
            }
            is EntryChapterList.Item -> RowGroupItem(position = positions[index] ?: RowGroupPosition.Alone) {
                EntryChapterListItem(
                    title = entryChapterTitle(entry, item.chapter),
                    date = entryChapterDate(item.chapter),
                    readProgress = childProgressLabels[item.chapter.id]
                        ?.let { stringResource(it.resource, *it.args.toTypedArray()) },
                    duration = readChapterDurations[item.chapter.id]
                        ?.takeIf { item.chapter.read }
                        ?.let(formatDuration),
                    scanlator = item.chapter.scanlator.takeIf { !it.isNullOrBlank() },
                    kind = entryChapterKindLabel(item),
                    summary = item.chapter.summary.takeIf { entry.showsChapterPreviews },
                    preview = entryChapterPreview(item).takeIf { entry.showsChapterPreviews },
                    onPreviewClick = { onChapterPreviewClicked(item) },
                    read = item.chapter.read,
                    bookmark = item.chapter.bookmark,
                    selected = item.selected,
                    downloadIndicatorEnabled = !isAnyChapterSelected && onDownloadChapter != null,
                    downloadStateProvider = { item.downloadState },
                    downloadProgressProvider = { downloadProgressProvider(item.id) },
                    highlightTrigger = chapterHighlightGeneration.takeIf {
                        item.chapter.id == highlightedChapterId
                    },
                    unconsumedIndicatorLabel = item.entry.type.entryTypePresentation().unconsumedIndicatorLabel,
                    chapterSwipeStartAction = chapterSwipeStartAction,
                    chapterSwipeEndAction = chapterSwipeEndAction,
                    onLongClick = {
                        onChapterSelected(item, !item.selected, true)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onClick = {
                        onChapterItemClick(
                            chapterItem = item,
                            isAnyChapterSelected = isAnyChapterSelected,
                            onToggleSelection = { onChapterSelected(item, !item.selected, false) },
                            onChapterClicked = onChapterClicked,
                        )
                    },
                    onDownloadClick = if (onDownloadChapter != null) {
                        { onDownloadChapter(listOf(item), it) }
                    } else {
                        null
                    },
                    onChapterSwipe = {
                        onChapterSwipe(item, it)
                    },
                    translateStatusProvider = { translation?.statuses?.get(item.id) },
                    onTranslateClick = translation?.let { ui -> { action -> ui.onAction(listOf(item), action) } },
                )
            }
        }
    }
}

private fun onChapterItemClick(
    chapterItem: EntryChapterList.Item,
    isAnyChapterSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit,
    onChapterClicked: ((EntryChapter) -> Unit)?,
) {
    when {
        chapterItem.selected -> onToggleSelection(false)
        isAnyChapterSelected -> onToggleSelection(true)
        else -> onChapterClicked?.invoke(chapterItem.chapter)
    }
}

/** Several sources have no single name to search for, so their line opens Manage merge, which lists each member's. */
private fun sourceClickAction(
    state: EntryViewModel.State.Success,
    onManageMergeClicked: (() -> Unit)?,
    onMissingSourceClicked: (() -> Unit)?,
    onSearch: (query: String, global: Boolean) -> Unit,
): () -> Unit {
    return onManageMergeClicked?.takeIf { state.isMultiSource }
        ?: onMissingSourceClicked?.takeIf { state.isSourceMissing }
        ?: { onSearch(state.sourceSearchQuery, false) }
}
