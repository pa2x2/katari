package eu.kanade.presentation.library.components

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
import eu.kanade.core.preference.PreferenceMutableState
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
    searchQuery: String?,
    selection: Set<LibraryItemKey>,
    contentPadding: PaddingValues,
    currentPage: Int,
    hasActiveFilters: Boolean,
    showPageTabs: Boolean,
    showItemCounts: Boolean,
    onChangeCurrentPage: (Int) -> Unit,
    onRefresh: suspend () -> Boolean,
    onGlobalSearchClicked: () -> Unit,
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

            if (secondaryTabs.isNotEmpty()) {
                LibraryTabs(
                    tabs = secondaryTabs,
                    selectedTabId = activePage?.secondaryTab?.id,
                    showItemCounts = showItemCounts,
                    onTabItemClick = { selectedTab ->
                        val targetPageIndex = pages.indexOfFirst {
                            it.primaryTab.id == activePage?.primaryTab?.id && it.secondaryTab?.id == selectedTab.id
                        }
                        if (targetPageIndex < 0) return@LibraryTabs
                        scope.launch {
                            pagerState.animateScrollToPage(targetPageIndex)
                        }
                    },
                )
            }
            if (tertiaryTabs.isNotEmpty()) {
                val selectedPage = checkNotNull(activePage)
                LibraryTabs(
                    tabs = tertiaryTabs,
                    selectedTabId = selectedPage.tertiaryTab?.id,
                    showItemCounts = showItemCounts,
                    onTabItemClick = { selectedTab ->
                        val targetPageIndex = pages.indexOfFirst {
                            it.primaryTab.id == selectedPage.primaryTab.id &&
                                it.secondaryTab?.id == selectedPage.secondaryTab?.id &&
                                it.tertiaryTab?.id == selectedTab.id
                        }
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
                    searchQuery = searchQuery,
                    hasActiveFilters = hasActiveFilters,
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                    onGlobalSearchClicked = onGlobalSearchClicked,
                )
                return@PullRefresh
            }
            pageContent(pagerState, pagerState.currentPage, pages.getOrNull(pagerState.currentPage))
        }

        LaunchedEffect(pagerState.settledPage) {
            onChangeCurrentPage(pagerState.settledPage)
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
    displaySettings: LibraryDisplaySettings,
) {
    SharedLibraryContent(
        pages = pages,
        searchQuery = searchQuery,
        selection = selection,
        contentPadding = contentPadding,
        currentPage = currentPage,
        hasActiveFilters = hasActiveFilters,
        showPageTabs = showPageTabs,
        showItemCounts = showItemCounts,
        onChangeCurrentPage = onChangeCurrentPage,
        onRefresh = onRefresh,
        onGlobalSearchClicked = onGlobalSearchClicked,
    ) { pagerState, _, _ ->
        LibraryPager(
            state = pagerState,
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
            hasActiveFilters = hasActiveFilters,
            selection = selection,
            searchQuery = searchQuery,
            onGlobalSearchClicked = onGlobalSearchClicked,
            getPageForIndex = { page -> pages[page] },
            getDisplayMode = getDisplayMode,
            getColumnsForOrientation = getColumnsForOrientation,
            getItemsForPage = getItemsForPage,
            displaySettings = displaySettings,
            onClickItem = { page, item ->
                if (selection.isNotEmpty()) {
                    onToggleSelection(page, item)
                } else {
                    onClickItem(item)
                }
            },
            onLongClickItem = { page, item ->
                if (selection.isEmpty()) {
                    onToggleSelection(page, item)
                } else {
                    onToggleRangeSelection(page, item)
                }
            },
            onClickContinueReading = onContinueReadingClicked,
            isContinueReadingAvailable = isContinueReadingAvailable,
        )
    }
}
