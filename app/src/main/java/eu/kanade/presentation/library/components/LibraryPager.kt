package eu.kanade.presentation.library.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.tachiyomi.ui.library.LibraryPage
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.FastScrollLazyColumn
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.util.plus

@Composable
fun LibraryPager(
    state: PagerState,
    contentPadding: PaddingValues,
    hasActiveFilters: Boolean,
    selection: Set<LibraryItemKey>,
    getPageForIndex: (Int) -> LibraryPage,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
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
        val columns by if (
            displayMode != LibraryDisplayMode.List &&
            displayMode != LibraryDisplayMode.ComfortableList
        ) {
            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            remember(isLandscape) { getColumnsForOrientation(isLandscape) }
        } else {
            remember { mutableIntStateOf(0) }
        }

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
                    columns = columns,
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
                    columns = columns,
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
                    columns = 1,
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
    val msg = if (hasActiveFilters) MR.strings.error_no_match else MR.strings.information_no_manga_group

    // A lazy column so an empty page still drives pull-to-refresh and the collapsing group header.
    FastScrollLazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding + PaddingValues(8.dp),
    ) {
        item {
            EmptyScreen(
                stringRes = msg,
                modifier = Modifier.fillParentMaxSize(),
            )
        }
    }
}
