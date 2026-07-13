package eu.kanade.presentation.entry

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import eu.kanade.presentation.components.AppSnackbarHost
import eu.kanade.presentation.components.MissingChapterCountListItem
import eu.kanade.presentation.components.relativeDateText
import eu.kanade.presentation.entry.components.ChapterDownloadAction
import eu.kanade.presentation.entry.components.EntryActionRow
import eu.kanade.presentation.entry.components.EntryBottomActionMenu
import eu.kanade.presentation.entry.components.EntryChapterHeader
import eu.kanade.presentation.entry.components.EntryChapterListItem
import eu.kanade.presentation.entry.components.EntryInfoBox
import eu.kanade.presentation.entry.components.EntryToolbar
import eu.kanade.presentation.entry.components.ExpandableEntryDescription
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.ui.entry.EntryChapterList
import eu.kanade.tachiyomi.ui.entry.EntryScreenModel
import eu.kanade.tachiyomi.ui.entry.entrySelectionActionLabels
import eu.kanade.tachiyomi.util.system.copyToClipboard
import mihon.entry.interactions.EntryChildProgressLabel
import mihon.entry.interactions.EntryDownloadState
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.missingChaptersCount
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ListGroupHeader
import tachiyomi.presentation.core.components.TwoPanelBox
import tachiyomi.presentation.core.components.VerticalFastScroller
import tachiyomi.presentation.core.components.material.PullRefresh
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.shouldExpandFAB
import tachiyomi.source.local.LocalSource
import java.time.Instant

@Composable
fun EntryScreen(
    state: EntryScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    isTabletUi: Boolean,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    navigateUp: () -> Unit,
    onChapterClicked: (EntryChapter) -> Unit,
    onDownloadChapter: ((List<EntryChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onAddToMergeClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onDuplicatesClicked: (() -> Unit)?,

    // For tags menu
    onTagSearch: (String) -> Unit,

    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditFetchIntervalClicked: (() -> Unit)?,
    onEditDisplayNameClicked: (() -> Unit)?,
    onManageMergeClicked: (() -> Unit)?,
    onOpenMergedEntryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,

    // For bottom action menu
    onMultiBookmarkClicked: (List<EntryChapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<EntryChapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (EntryChapter) -> Unit,
    onMultiDeleteClicked: (List<EntryChapter>) -> Unit,

    // For chapter swipe
    onChapterSwipe: (EntryChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,

    // Chapter selection
    onChapterSelected: (EntryChapterList.Item, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    entryPreviewEnabled: Boolean,
    entryPreviewSize: eu.kanade.presentation.entry.components.PreviewSizeUi,
    entryPreviewState: EntryScreenModel.EntryPreviewState,
    onPreviewExpandedChange: (Boolean) -> Unit,
    onPreviewRetry: () -> Unit,
    onPreviewPageLoad: (Int) -> Unit,
    onPreviewPageClick: (Long, Int) -> Unit,
) {
    val context = LocalContext.current
    val onCopyTagToClipboard: (tag: String) -> Unit = {
        if (it.isNotEmpty()) {
            context.copyToClipboard(it, it)
        }
    }

    if (!isTabletUi) {
        EntryScreenSmallImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            nextUpdate = nextUpdate,
            chapterSwipeStartAction = chapterSwipeStartAction,
            chapterSwipeEndAction = chapterSwipeEndAction,
            navigateUp = navigateUp,
            onChapterClicked = onChapterClicked,
            onDownloadChapter = onDownloadChapter,
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
            onEditCategoryClicked = onEditCategoryClicked,
            onEditIntervalClicked = onEditFetchIntervalClicked,
            onEditDisplayNameClicked = onEditDisplayNameClicked,
            onManageMergeClicked = onManageMergeClicked,
            onOpenMergedEntryClicked = onOpenMergedEntryClicked,
            onMigrateClicked = onMigrateClicked,
            onEditNotesClicked = onEditNotesClicked,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
            onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onChapterSwipe = onChapterSwipe,
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
        )
    } else {
        EntryScreenLargeImpl(
            state = state,
            snackbarHostState = snackbarHostState,
            chapterSwipeStartAction = chapterSwipeStartAction,
            chapterSwipeEndAction = chapterSwipeEndAction,
            nextUpdate = nextUpdate,
            navigateUp = navigateUp,
            onChapterClicked = onChapterClicked,
            onDownloadChapter = onDownloadChapter,
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
            onEditCategoryClicked = onEditCategoryClicked,
            onEditIntervalClicked = onEditFetchIntervalClicked,
            onEditDisplayNameClicked = onEditDisplayNameClicked,
            onManageMergeClicked = onManageMergeClicked,
            onOpenMergedEntryClicked = onOpenMergedEntryClicked,
            onMigrateClicked = onMigrateClicked,
            onEditNotesClicked = onEditNotesClicked,
            onMultiBookmarkClicked = onMultiBookmarkClicked,
            onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
            onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
            onMultiDeleteClicked = onMultiDeleteClicked,
            onChapterSwipe = onChapterSwipe,
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
        )
    }
}

@Composable
private fun EntryScreenSmallImpl(
    state: EntryScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    navigateUp: () -> Unit,
    onChapterClicked: (EntryChapter) -> Unit,
    onDownloadChapter: ((List<EntryChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onAddToMergeClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onDuplicatesClicked: (() -> Unit)?,

    // For tags menu
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,

    onFilterClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditIntervalClicked: (() -> Unit)?,
    onEditDisplayNameClicked: (() -> Unit)?,
    onManageMergeClicked: (() -> Unit)?,
    onOpenMergedEntryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,

    // For bottom action menu
    onMultiBookmarkClicked: (List<EntryChapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<EntryChapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (EntryChapter) -> Unit,
    onMultiDeleteClicked: (List<EntryChapter>) -> Unit,

    // For chapter swipe
    onChapterSwipe: (EntryChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,

    // Chapter selection
    onChapterSelected: (EntryChapterList.Item, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    entryPreviewEnabled: Boolean,
    entryPreviewSize: eu.kanade.presentation.entry.components.PreviewSizeUi,
    entryPreviewState: EntryScreenModel.EntryPreviewState,
    onPreviewExpandedChange: (Boolean) -> Unit,
    onPreviewRetry: () -> Unit,
    onPreviewPageLoad: (Int) -> Unit,
    onPreviewPageClick: (Long, Int) -> Unit,
) {
    val chapterListState = rememberLazyListState()

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
            val selectedChapterCount: Int = remember(chapters) {
                chapters.count { it.selected }
            }
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
                title = state.entry.displayTitle,
                hasFilters = state.filterActive,
                navigateUp = navigateUp,
                onClickFilter = onFilterClicked,
                onClickShare = onShareClicked,
                onClickDownload = onDownloadActionClicked,
                onClickEditCategory = onEditCategoryClicked,
                onClickEditDisplayName = onEditDisplayNameClicked,
                onClickManageMerge = onManageMergeClicked,
                onClickRefresh = onRefresh,
                onClickMigrate = onMigrateClicked,
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
            val selectedChapters = remember(chapters) {
                chapters.filter { it.selected }
            }
            SharedEntryBottomActionMenu(
                selected = selectedChapters,
                childProgressLabels = state.childProgressLabels,
                onMultiBookmarkClicked = onMultiBookmarkClicked,
                onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
                onDownloadChapter = onDownloadChapter,
                onMultiDeleteClicked = onMultiDeleteClicked,
                fillFraction = 1f,
            )
        },
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val isFABVisible = remember(chapters) {
                chapters.fastAny { !it.chapter.read } && !isAnySelected
            }
            SmallExtendedFloatingActionButton(
                text = {
                    val isReading = remember(state.chapters) {
                        state.chapters.fastAny { it.chapter.read }
                    }
                    Text(
                        text = stringResource(if (isReading) MR.strings.action_resume else MR.strings.action_start),
                    )
                },
                icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
                onClick = onContinueReading,
                expanded = chapterListState.shouldExpandFAB(),
                modifier = Modifier.animateFloatingActionButton(
                    visible = isFABVisible,
                    alignment = Alignment.BottomEnd,
                ),
            )
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
                            mergedMemberTitles = state.mergedMemberTitles,
                            onCoverClick = onCoverClicked,
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
                        EntryActionRow(
                            favorite = state.entry.favorite,
                            trackingCount = state.trackingCount,
                            nextUpdate = nextUpdate,
                            isUserIntervalMode = state.entry.fetchInterval < 0,
                            onAddToLibraryClicked = onAddToLibraryClicked,
                            onAddToMergeClicked = onAddToMergeClicked,
                            onWebViewClicked = onWebViewClicked,
                            onWebViewLongClicked = onWebViewLongClicked,
                            onTrackingClicked = onTrackingClicked,
                            onDuplicatesClicked = onDuplicatesClicked,
                            onEditIntervalClicked = onEditIntervalClicked,
                            onEditCategory = onEditCategoryClicked,
                        )
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

                    item(
                        key = EntryScreenItem.CHAPTER_HEADER,
                        contentType = EntryScreenItem.CHAPTER_HEADER,
                    ) {
                        val missingChapterCount = remember(chapters) {
                            chapters.map { it.chapter.chapterNumber }.missingChaptersCount()
                        }
                        EntryChapterHeader(
                            enabled = !isAnySelected,
                            entryType = state.entry.type,
                            chapterCount = chapters.size,
                            missingChapterCount = missingChapterCount,
                            onClick = onFilterClicked,
                        )
                    }

                    sharedChapterItems(
                        entry = state.entry,
                        mergedMemberIds = state.memberIds,
                        memberTitleById = state.memberTitleById,
                        chapters = listItem,
                        childProgressLabels = state.childProgressLabels,
                        isAnyChapterSelected = chapters.fastAny { it.selected },
                        chapterSwipeStartAction = chapterSwipeStartAction,
                        chapterSwipeEndAction = chapterSwipeEndAction,
                        onChapterClicked = onChapterClicked,
                        onDownloadChapter = onDownloadChapter,
                        onChapterSelected = onChapterSelected,
                        onChapterSwipe = onChapterSwipe,
                    )
                }
            }
        }
    }
}

@Composable
fun EntryScreenLargeImpl(
    state: EntryScreenModel.State.Success,
    snackbarHostState: SnackbarHostState,
    nextUpdate: Instant?,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    navigateUp: () -> Unit,
    onChapterClicked: (EntryChapter) -> Unit,
    onDownloadChapter: ((List<EntryChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onAddToLibraryClicked: () -> Unit,
    onAddToMergeClicked: (() -> Unit)?,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onDuplicatesClicked: (() -> Unit)?,

    // For tags menu
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,

    onFilterButtonClicked: () -> Unit,
    onRefresh: () -> Unit,
    onContinueReading: () -> Unit,
    onSearch: (query: String, global: Boolean) -> Unit,

    // For cover dialog
    onCoverClicked: () -> Unit,

    // For top action menu
    onShareClicked: (() -> Unit)?,
    onDownloadActionClicked: ((DownloadAction) -> Unit)?,
    onEditCategoryClicked: (() -> Unit)?,
    onEditIntervalClicked: (() -> Unit)?,
    onEditDisplayNameClicked: (() -> Unit)?,
    onManageMergeClicked: (() -> Unit)?,
    onOpenMergedEntryClicked: (() -> Unit)?,
    onMigrateClicked: (() -> Unit)?,
    onEditNotesClicked: () -> Unit,

    // For bottom action menu
    onMultiBookmarkClicked: (List<EntryChapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<EntryChapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (EntryChapter) -> Unit,
    onMultiDeleteClicked: (List<EntryChapter>) -> Unit,

    // For swipe actions
    onChapterSwipe: (EntryChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,

    // Chapter selection
    onChapterSelected: (EntryChapterList.Item, Boolean, Boolean) -> Unit,
    onAllChapterSelected: (Boolean) -> Unit,
    onInvertSelection: () -> Unit,
    entryPreviewEnabled: Boolean,
    entryPreviewSize: eu.kanade.presentation.entry.components.PreviewSizeUi,
    entryPreviewState: EntryScreenModel.EntryPreviewState,
    onPreviewExpandedChange: (Boolean) -> Unit,
    onPreviewRetry: () -> Unit,
    onPreviewPageLoad: (Int) -> Unit,
    onPreviewPageClick: (Long, Int) -> Unit,
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

    BackHandler(enabled = isAnySelected) {
        onAllChapterSelected(false)
    }

    Scaffold(
        topBar = {
            val selectedChapterCount = remember(chapters) {
                chapters.count { it.selected }
            }
            EntryToolbar(
                modifier = Modifier.onSizeChanged { topBarHeight = it.height },
                title = state.entry.displayTitle,
                hasFilters = state.filterActive,
                navigateUp = navigateUp,
                onClickFilter = onFilterButtonClicked,
                onClickShare = onShareClicked,
                onClickDownload = onDownloadActionClicked,
                onClickEditCategory = onEditCategoryClicked,
                onClickEditDisplayName = onEditDisplayNameClicked,
                onClickManageMerge = onManageMergeClicked,
                onClickRefresh = onRefresh,
                onClickMigrate = onMigrateClicked,
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
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomEnd,
            ) {
                val selectedChapters = remember(chapters) {
                    chapters.filter { it.selected }
                }
                SharedEntryBottomActionMenu(
                    selected = selectedChapters,
                    childProgressLabels = state.childProgressLabels,
                    onMultiBookmarkClicked = onMultiBookmarkClicked,
                    onMultiMarkAsReadClicked = onMultiMarkAsReadClicked,
                    onMarkPreviousAsReadClicked = onMarkPreviousAsReadClicked,
                    onDownloadChapter = onDownloadChapter,
                    onMultiDeleteClicked = onMultiDeleteClicked,
                    fillFraction = 0.5f,
                )
            }
        },
        snackbarHost = { AppSnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            val isFABVisible = remember(chapters) {
                chapters.fastAny { !it.chapter.read } && !isAnySelected
            }
            SmallExtendedFloatingActionButton(
                text = {
                    val isReading = remember(state.chapters) {
                        state.chapters.fastAny { it.chapter.read }
                    }
                    Text(
                        text = stringResource(
                            if (isReading) MR.strings.action_resume else MR.strings.action_start,
                        ),
                    )
                },
                icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null) },
                onClick = onContinueReading,
                expanded = chapterListState.shouldExpandFAB(),
                modifier = Modifier.animateFloatingActionButton(
                    visible = isFABVisible,
                    alignment = Alignment.BottomEnd,
                ),
            )
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
                            mergedMemberTitles = state.mergedMemberTitles,
                            onCoverClick = onCoverClicked,
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
                            nextUpdate = nextUpdate,
                            isUserIntervalMode = state.entry.fetchInterval < 0,
                            onAddToLibraryClicked = onAddToLibraryClicked,
                            onAddToMergeClicked = onAddToMergeClicked,
                            onWebViewClicked = onWebViewClicked,
                            onWebViewLongClicked = onWebViewLongClicked,
                            onTrackingClicked = onTrackingClicked,
                            onDuplicatesClicked = onDuplicatesClicked,
                            onEditIntervalClicked = onEditIntervalClicked,
                            onEditCategory = onEditCategoryClicked,
                        )
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
                            item(
                                key = EntryScreenItem.CHAPTER_HEADER,
                                contentType = EntryScreenItem.CHAPTER_HEADER,
                            ) {
                                val missingChapterCount = remember(chapters) {
                                    chapters.map { it.chapter.chapterNumber }.missingChaptersCount()
                                }
                                EntryChapterHeader(
                                    enabled = !isAnySelected,
                                    entryType = state.entry.type,
                                    chapterCount = chapters.size,
                                    missingChapterCount = missingChapterCount,
                                    onClick = onFilterButtonClicked,
                                )
                            }

                            sharedChapterItems(
                                entry = state.entry,
                                mergedMemberIds = state.memberIds,
                                memberTitleById = state.memberTitleById,
                                chapters = listItem,
                                childProgressLabels = state.childProgressLabels,
                                isAnyChapterSelected = chapters.fastAny { it.selected },
                                chapterSwipeStartAction = chapterSwipeStartAction,
                                chapterSwipeEndAction = chapterSwipeEndAction,
                                onChapterClicked = onChapterClicked,
                                onDownloadChapter = onDownloadChapter,
                                onChapterSelected = onChapterSelected,
                                onChapterSwipe = onChapterSwipe,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun MergeNotice(
    onOpenMergedEntry: (() -> Unit)?,
    onManageMerge: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(MR.strings.merge_member_notice),
                style = MaterialTheme.typography.bodyMedium,
            )
            onOpenMergedEntry?.let {
                FilledTonalButton(
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(MR.strings.action_open_merged_entry))
                }
            }
            onManageMerge?.let {
                OutlinedButton(
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(MR.strings.action_manage_merge))
                }
            }
        }
    }
}

@Composable
private fun SharedEntryBottomActionMenu(
    selected: List<EntryChapterList.Item>,
    childProgressLabels: Map<Long, EntryChildProgressLabel>,
    onMultiBookmarkClicked: (List<EntryChapter>, bookmarked: Boolean) -> Unit,
    onMultiMarkAsReadClicked: (List<EntryChapter>, markAsRead: Boolean) -> Unit,
    onMarkPreviousAsReadClicked: (EntryChapter) -> Unit,
    onDownloadChapter: ((List<EntryChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onMultiDeleteClicked: (List<EntryChapter>) -> Unit,
    fillFraction: Float,
    modifier: Modifier = Modifier,
) {
    EntryBottomActionMenu(
        visible = selected.isNotEmpty(),
        modifier = modifier.fillMaxWidth(fillFraction),
        onBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.chapter }, true)
        }.takeIf { selected.fastAny { !it.chapter.bookmark } },
        onRemoveBookmarkClicked = {
            onMultiBookmarkClicked.invoke(selected.fastMap { it.chapter }, false)
        }.takeIf { selected.fastAll { it.chapter.bookmark } },
        bookmarkLabel = selected.map { it.entry.type }.selectionEntryTypePresentation().bookmarkChildLabel,
        removeBookmarkLabel = selected.map { it.entry.type }.selectionEntryTypePresentation().removeBookmarkChildLabel,
        onMarkAsReadClicked = {
            onMultiMarkAsReadClicked(selected.fastMap { it.chapter }, true)
        }.takeIf { selected.fastAny { !it.chapter.read } },
        onMarkAsUnreadClicked = {
            onMultiMarkAsReadClicked(selected.fastMap { it.chapter }, false)
        }.takeIf { selected.fastAny { it.chapter.read || it.chapter.id in childProgressLabels } },
        markAsReadLabel = selected.map { it.entry.type }.entrySelectionActionLabels().markAsReadLabel,
        markAsUnreadLabel = selected.map { it.entry.type }.entrySelectionActionLabels().markAsUnreadLabel,
        markPreviousAsReadLabel = selected.map { it.entry.type }
            .selectionEntryTypePresentation()
            .markPreviousAsConsumedLabel,
        downloadPresentation = selected.map { it.entry.type }.selectionEntryTypePresentation(),
        onMarkPreviousAsReadClicked = {
            onMarkPreviousAsReadClicked(selected[0].chapter)
        }.takeIf { selected.size == 1 },
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
    )
}

private fun LazyListScope.sharedChapterItems(
    entry: Entry,
    mergedMemberIds: List<Long>,
    memberTitleById: Map<Long, String>,
    chapters: List<EntryChapterList>,
    childProgressLabels: Map<Long, EntryChildProgressLabel>,
    isAnyChapterSelected: Boolean,
    chapterSwipeStartAction: LibraryPreferences.ChapterSwipeAction,
    chapterSwipeEndAction: LibraryPreferences.ChapterSwipeAction,
    onChapterClicked: (EntryChapter) -> Unit,
    onDownloadChapter: ((List<EntryChapterList.Item>, ChapterDownloadAction) -> Unit)?,
    onChapterSelected: (EntryChapterList.Item, Boolean, Boolean) -> Unit,
    onChapterSwipe: (EntryChapterList.Item, LibraryPreferences.ChapterSwipeAction) -> Unit,
) {
    items(
        items = chapters,
        key = { item ->
            when (item) {
                is EntryChapterList.MemberHeader -> "member-header-${item.entryId}"
                is EntryChapterList.MissingCount -> "missing-count-${item.id}"
                is EntryChapterList.Item -> "chapter-${item.id}"
            }
        },
        contentType = {
            when (it) {
                is EntryChapterList.MemberHeader -> EntryScreenItem.CHAPTER_GROUP_HEADER
                else -> EntryScreenItem.CHAPTER
            }
        },
    ) { item ->
        val haptic = LocalHapticFeedback.current

        when (item) {
            is EntryChapterList.MemberHeader -> {
                ListGroupHeader(text = item.title)
            }
            is EntryChapterList.MissingCount -> {
                MissingChapterCountListItem(count = item.count, entryType = entry.type)
            }
            is EntryChapterList.Item -> {
                EntryChapterListItem(
                    title = if (entry.displayMode == Entry.CHAPTER_DISPLAY_NUMBER) {
                        stringResource(
                            entry.type.entryTypePresentation().childNumberDisplayLabel,
                            formatChapterNumber(item.chapter.chapterNumber),
                        )
                    } else {
                        item.chapter.name
                    },
                    date = relativeDateText(item.chapter.dateUpload),
                    readProgress = childProgressLabels[item.chapter.id]
                        ?.let { stringResource(it.resource, *it.args.toTypedArray()) },
                    scanlator = item.chapter.scanlator.takeIf { !it.isNullOrBlank() },
                    read = item.chapter.read,
                    bookmark = item.chapter.bookmark,
                    selected = item.selected,
                    downloadIndicatorEnabled = !isAnyChapterSelected && entry.source != LocalSource.ID,
                    downloadStateProvider = { item.downloadState },
                    downloadProgressProvider = { item.downloadProgress },
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
                )
            }
        }
    }
}

private fun onChapterItemClick(
    chapterItem: EntryChapterList.Item,
    isAnyChapterSelected: Boolean,
    onToggleSelection: (Boolean) -> Unit,
    onChapterClicked: (EntryChapter) -> Unit,
) {
    when {
        chapterItem.selected -> onToggleSelection(false)
        isAnyChapterSelected -> onToggleSelection(true)
        else -> onChapterClicked(chapterItem.chapter)
    }
}
