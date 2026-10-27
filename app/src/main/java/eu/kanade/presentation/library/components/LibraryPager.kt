package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.tachiyomi.ui.library.LibraryPage
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.feedback.NoResults
import mihon.presentation.kit.layout.coverGridCells
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.domain.library.model.CoverSize
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus

@Composable
fun LibraryPager(
    state: PagerState,
    contentPadding: PaddingValues,
    hasActiveFilters: Boolean,
    selection: Set<LibraryItemKey>,
    getPageForIndex: (Int) -> LibraryPage,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    coverSize: CoverSize,
    getItemsForPage: (LibraryPage) -> List<LibraryItem>,
    displaySettingsForPage: (LibraryPage) -> LibraryDisplaySettings,
    onClickItem: (LibraryPage, LibraryItem) -> Unit,
    onLongClickItem: (LibraryPage, LibraryItem) -> Unit,
    onClickContinueReading: ((LibraryItem) -> Unit)?,
    isContinueReadingAvailable: (LibraryItem) -> Boolean,
    scrollToTopTarget: LibraryScrollToTopTarget,
) {
    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        state = state,
        verticalAlignment = Alignment.Top,
    ) { page ->
        if (page !in ((state.currentPage - 1)..(state.currentPage + 1))) {
            // To make sure only one offscreen page is being composed
            return@HorizontalPager
        }
        val libraryPage = getPageForIndex(page)
        val items = getItemsForPage(libraryPage)

        if (items.isEmpty()) {
            LibraryPageEmptyScreen(
                hasActiveFilters = hasActiveFilters,
                contentPadding = contentPadding,
            )
            return@HorizontalPager
        }

        val displayMode by getDisplayMode(page)
        val displaySettings = displaySettingsForPage(libraryPage)
        val pageScrollToTopTarget = scrollToTopTarget.takeIf { page == state.currentPage }
        val onClick: (LibraryItem) -> Unit = { onClickItem(libraryPage, it) }
        val onLongClick: (LibraryItem) -> Unit = { onLongClickItem(libraryPage, it) }

        when (displayMode) {
            LibraryDisplayMode.List -> {
                LibraryList(
                    items = items,
                    contentPadding = contentPadding,
                    selection = selection,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onClickContinueReading = onClickContinueReading,
                    isContinueReadingAvailable = isContinueReadingAvailable,
                    displaySettings = displaySettings,
                    scrollToTopTarget = pageScrollToTopTarget,
                )
            }
            LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> {
                LibraryCompactGrid(
                    items = items,
                    showTitle = displayMode is LibraryDisplayMode.CompactGrid,
                    columns = coverGridCells(coverSize),
                    contentPadding = contentPadding,
                    selection = selection,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onClickContinueReading = onClickContinueReading,
                    isContinueReadingAvailable = isContinueReadingAvailable,
                    displaySettings = displaySettings,
                    scrollToTopTarget = pageScrollToTopTarget,
                )
            }
            LibraryDisplayMode.ComfortableGrid -> {
                LibraryComfortableGrid(
                    items = items,
                    columns = coverGridCells(coverSize),
                    contentPadding = contentPadding,
                    selection = selection,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onClickContinueReading = onClickContinueReading,
                    isContinueReadingAvailable = isContinueReadingAvailable,
                    displaySettings = displaySettings,
                    scrollToTopTarget = pageScrollToTopTarget,
                )
            }
            LibraryDisplayMode.ComfortableList -> {
                LibraryComfortableGrid(
                    items = items,
                    columns = GridCells.Fixed(1),
                    contentPadding = contentPadding,
                    selection = selection,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onClickContinueReading = onClickContinueReading,
                    isContinueReadingAvailable = isContinueReadingAvailable,
                    displaySettings = displaySettings,
                    scrollToTopTarget = pageScrollToTopTarget,
                )
            }
        }
    }
}

@Composable
fun LibraryPageEmptyScreen(
    hasActiveFilters: Boolean,
    contentPadding: PaddingValues,
) {
    // A lazy column so an empty page still drives pull-to-refresh and the collapsing group header.
    FastScrollLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(KatariTheme.spacing.s),
    ) {
        item {
            if (hasActiveFilters) {
                Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    NoResults(message = stringResource(MR.strings.error_no_match))
                }
            } else {
                EmptyScreen(
                    stringRes = MR.strings.information_no_manga_group,
                    modifier = Modifier.fillParentMaxSize(),
                )
            }
        }
    }
}
