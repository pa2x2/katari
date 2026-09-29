package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.presentation.library.search.LibrarySearchResults
import eu.kanade.presentation.library.search.LibrarySearchTips
import eu.kanade.tachiyomi.ui.library.LibraryPage
import eu.kanade.tachiyomi.ui.library.LibraryPageTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.presentation.core.components.material.PullRefresh
import kotlin.time.Duration.Companion.seconds

@Composable
fun SharedLibraryContent(
    pages: List<LibraryPage>,
    selection: Set<LibraryItemKey>,
    contentPadding: PaddingValues,
    currentPage: Int,
    hasActiveFilters: Boolean,
    showPageTabs: Boolean,
    showItemCounts: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onRefresh: suspend () -> Boolean,
    scrollToTopTarget: LibraryScrollToTopTarget,
    pageContent: @Composable (pagerState: PagerState, page: Int, libraryPage: LibraryPage?) -> Unit,
) {
    Column(
        modifier = Modifier.padding(
            top = contentPadding.calculateTopPadding(),
            start = contentPadding.calculateStartPadding(LocalLayoutDirection.current),
            end = contentPadding.calculateEndPadding(LocalLayoutDirection.current),
        ),
    ) {
        val pagerState = rememberPagerState(currentPage) { pages.size }

        val scope = rememberCoroutineScope()
        var isRefreshing by remember(pagerState.currentPage) { mutableStateOf(false) }

        val primaryTabs = remember(pages) {
            pages.map(LibraryPage::primaryTab).distinctBy(LibraryPageTab::id)
        }
        val activePage = pages.getOrNull(pagerState.currentPage)
        val secondaryTabs = remember(pages, activePage?.primaryTab?.id) {
            activePage?.primaryTab?.id
                ?.let { primaryTabId ->
                    pages.filter { it.primaryTab.id == primaryTabId }
                        .mapNotNull(LibraryPage::secondaryTab)
                        .distinctBy(LibraryPageTab::id)
                }
                .orEmpty()
        }
        val tertiaryTabs = remember(
            pages,
            activePage?.primaryTab?.id,
            activePage?.secondaryTab?.id,
        ) {
            if (activePage?.secondaryTab == null) {
                emptyList()
            } else {
                pages.filter {
                    it.primaryTab.id == activePage.primaryTab.id &&
                        it.secondaryTab?.id == activePage.secondaryTab.id
                }
                    .mapNotNull(LibraryPage::tertiaryTab)
                    .distinctBy(LibraryPageTab::id)
            }
        }

        if (showPageTabs && pages.isNotEmpty()) {
            LaunchedEffect(pages) {
                if (pages.size <= pagerState.currentPage) {
                    pagerState.scrollToPage(pages.size - 1)
                }
            }

            if (primaryTabs.size > 1 || secondaryTabs.isNotEmpty() || tertiaryTabs.isNotEmpty()) {
                LibraryTabs(
                    tabs = primaryTabs,
                    selectedTabId = activePage?.primaryTab?.id,
                    showItemCounts = showItemCounts,
                    onTabItemClick = { selectedTab ->
                        val targetPageIndex = pages.indexOfFirst { it.primaryTab.id == selectedTab.id }
                        if (targetPageIndex < 0) return@LibraryTabs
                        scope.launch {
                            pagerState.animateScrollToPage(targetPageIndex)
                        }
                    },
                )
            }
        }

        PullRefresh(
            refreshing = isRefreshing,
            enabled = selection.isEmpty(),
            onRefresh = {
                scope.launch {
                    val started = onRefresh()
                    if (!started) return@launch
                    // Fake refresh status but hide it after a second as it's a long running task
                    isRefreshing = true
                    delay(1.seconds)
                    isRefreshing = false
                }
            },
        ) {
            if (pages.isEmpty()) {
                LibraryPageEmptyScreen(
                    hasActiveFilters = hasActiveFilters,
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                )
                return@PullRefresh
            }
            val chipLevels = if (showPageTabs) {
                buildList {
                    if (secondaryTabs.isNotEmpty()) {
                        add(
                            LibraryGroupChipLevel(
                                tabs = secondaryTabs,
                                selectedTabId = activePage?.secondaryTab?.id,
                                onSelect = { selectedTab ->
                                    val targetPageIndex = pages.indexOfFirst {
                                        it.primaryTab.id == activePage?.primaryTab?.id &&
                                            it.secondaryTab?.id == selectedTab.id
                                    }
                                    if (targetPageIndex >= 0) {
                                        scope.launch { pagerState.animateScrollToPage(targetPageIndex) }
                                    }
                                },
                            ),
                        )
                    }
                    if (tertiaryTabs.isNotEmpty() && activePage != null) {
                        add(
                            LibraryGroupChipLevel(
                                tabs = tertiaryTabs,
                                selectedTabId = activePage.tertiaryTab?.id,
                                onSelect = { selectedTab ->
                                    val targetPageIndex = pages.indexOfFirst {
                                        it.primaryTab.id == activePage.primaryTab.id &&
                                            it.secondaryTab?.id == activePage.secondaryTab?.id &&
                                            it.tertiaryTab?.id == selectedTab.id
                                    }
                                    if (targetPageIndex >= 0) {
                                        scope.launch { pagerState.animateScrollToPage(targetPageIndex) }
                                    }
                                },
                            ),
                        )
                    }
                }
            } else {
                emptyList()
            }
            // Lower grouping levels scroll away with the grid instead of permanently stacking tab rows.
            val headerState = rememberLibraryCollapsingHeaderState()
            BindHeaderToTop(scrollToTopTarget, headerState)
            LaunchedEffect(pagerState.settledPage) { headerState.expand() }
            LibraryCollapsingHeader(
                state = headerState,
                header = {
                    if (chipLevels.isNotEmpty()) {
                        LibraryGroupChipRows(
                            levels = chipLevels,
                            showItemCounts = showItemCounts,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                },
            ) {
                pageContent(pagerState, pagerState.currentPage, pages.getOrNull(pagerState.currentPage))
            }
        }

        LaunchedEffect(pagerState.settledPage) {
            onChangeCurrentPage(pagerState.settledPage)
        }
        // When the pages are regrouped (grouping, filters, leaving search) the model re-resolves the active page;
        // follow it instead of keeping a stale index into the new list.
        LaunchedEffect(currentPage, pages) {
            if (currentPage in pages.indices && currentPage != pagerState.settledPage &&
                !pagerState.isScrollInProgress
            ) {
                pagerState.scrollToPage(currentPage)
            }
        }
    }
}

@Composable
fun LibraryContent(
    pages: List<LibraryPage>,
    searchQuery: String?,
    selection: Set<LibraryItemKey>,
    contentPadding: PaddingValues,
    currentPage: Int,
    hasActiveFilters: Boolean,
    showPageTabs: Boolean,
    showItemCounts: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onClickItem: (LibraryItem) -> Unit,
    onContinueReadingClicked: ((LibraryItem) -> Unit)?,
    isContinueReadingAvailable: (LibraryItem) -> Boolean,
    onToggleSelection: (LibraryPage, LibraryItem) -> Unit,
    onToggleRangeSelection: (LibraryPage, LibraryItem) -> Unit,
    onRefresh: suspend () -> Boolean,
    onGlobalSearchClicked: () -> Unit,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    getItemsForPage: (LibraryPage) -> List<LibraryItem>,
    displaySettingsForPage: (LibraryPage) -> LibraryDisplaySettings,
    scrollToTopTarget: LibraryScrollToTopTarget,
    showSearchTips: Boolean,
    onSearchQueryChange: (String?) -> Unit,
    onSeeAllSearchResults: (LibraryPage) -> Unit,
) {
    val onClickPageItem: (LibraryPage, LibraryItem) -> Unit = { page, item ->
        if (selection.isNotEmpty()) {
            onToggleSelection(page, item)
        } else {
            onClickItem(item)
        }
    }
    val onLongClickPageItem: (LibraryPage, LibraryItem) -> Unit = { page, item ->
        if (selection.isEmpty()) {
            onToggleSelection(page, item)
        } else {
            onToggleRangeSelection(page, item)
        }
    }

    if (!searchQuery.isNullOrEmpty()) {
        LibrarySearchResults(
            pages = pages,
            searchQuery = searchQuery,
            contentPadding = contentPadding,
            selection = selection,
            getItemsForPage = getItemsForPage,
            getDisplayMode = getDisplayMode,
            getColumnsForOrientation = getColumnsForOrientation,
            displaySettingsForPage = displaySettingsForPage,
            onClickItem = onClickPageItem,
            onLongClickItem = onLongClickPageItem,
            onClickContinueReading = onContinueReadingClicked,
            isContinueReadingAvailable = isContinueReadingAvailable,
            onSeeAll = onSeeAllSearchResults,
            onGlobalSearchClicked = onGlobalSearchClicked,
            scrollToTopTarget = scrollToTopTarget,
        )
        return
    }

    Box {
        SharedLibraryContent(
            pages = pages,
            selection = selection,
            contentPadding = contentPadding,
            currentPage = currentPage,
            hasActiveFilters = hasActiveFilters,
            showPageTabs = showPageTabs,
            showItemCounts = showItemCounts,
            onChangeCurrentPage = onChangeCurrentPage,
            onRefresh = onRefresh,
            scrollToTopTarget = scrollToTopTarget,
        ) { pagerState, _, _ ->
            LibraryPager(
                state = pagerState,
                contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                hasActiveFilters = hasActiveFilters,
                selection = selection,
                getPageForIndex = { page -> pages[page] },
                getDisplayMode = getDisplayMode,
                getColumnsForOrientation = getColumnsForOrientation,
                getItemsForPage = getItemsForPage,
                displaySettingsForPage = displaySettingsForPage,
                onClickItem = onClickPageItem,
                onLongClickItem = onLongClickPageItem,
                onClickContinueReading = onContinueReadingClicked,
                isContinueReadingAvailable = isContinueReadingAvailable,
                scrollToTopTarget = scrollToTopTarget,
            )
        }
        // An open but empty search field: show what the query language can do.
        if (showSearchTips) {
            LibrarySearchTips(
                onInsertToken = onSearchQueryChange,
                modifier = Modifier.padding(
                    top = contentPadding.calculateTopPadding() + 8.dp,
                    start = 12.dp,
                    end = 12.dp,
                ),
            )
        }
    }
}
