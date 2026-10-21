package eu.kanade.presentation.library.search

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import eu.kanade.core.preference.PreferenceMutableState
import eu.kanade.presentation.library.components.BindScrollToTop
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.presentation.library.components.GlobalSearchItem
import eu.kanade.presentation.library.components.LibraryComfortableGridEntry
import eu.kanade.presentation.library.components.LibraryCompactGridEntry
import eu.kanade.presentation.library.components.LibraryDisplaySettings
import eu.kanade.presentation.library.components.LibraryListEntry
import eu.kanade.presentation.library.components.LibraryScrollToTopTarget
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.ui.library.LibraryPage
import eu.kanade.tachiyomi.ui.library.displayTitle
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.TravelExplore
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.BrowseMetrics
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.feedback.EmptyScreen
import mihon.presentation.kit.list.RowGroupHeader
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.domain.library.model.LibraryDisplayMode
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.FastScrollLazyVerticalGrid
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import kotlin.math.floor

/**
 * Search results from every library page in one scroll: each page with matches gets a header and a short preview,
 * and "See all" opens that page so its full results stay one tap away without swiping through tabs.
 */
@Composable
internal fun LibrarySearchResults(
    pages: List<LibraryPage>,
    searchQuery: String,
    contentPadding: PaddingValues,
    selection: Set<LibraryItemKey>,
    getItemsForPage: (LibraryPage) -> List<LibraryItem>,
    getDisplayMode: (Int) -> PreferenceMutableState<LibraryDisplayMode>,
    getColumnsForOrientation: (Boolean) -> PreferenceMutableState<Int>,
    displaySettingsForPage: (LibraryPage) -> LibraryDisplaySettings,
    onClickItem: (LibraryPage, LibraryItem) -> Unit,
    onLongClickItem: (LibraryPage, LibraryItem) -> Unit,
    onClickContinueReading: ((LibraryItem) -> Unit)?,
    isContinueReadingAvailable: (LibraryItem) -> Boolean,
    onSeeAll: (LibraryPage) -> Unit,
    onGlobalSearchClicked: () -> Unit,
    scrollToTopTarget: LibraryScrollToTopTarget,
    modifier: Modifier = Modifier,
) {
    val sections = pages
        .map { page -> page to getItemsForPage(page) }
        .filter { (_, items) -> items.isNotEmpty() }
    if (sections.isEmpty()) {
        LibrarySearchNoResults(
            searchQuery = searchQuery,
            contentPadding = contentPadding,
            onGlobalSearchClicked = onGlobalSearchClicked,
            modifier = modifier,
        )
        return
    }
    val displayMode by getDisplayMode(0)
    val isListMode = displayMode == LibraryDisplayMode.List || displayMode == LibraryDisplayMode.ComfortableList
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val columnsPreference = remember(isLandscape) { getColumnsForOrientation(isLandscape) }
    val preferredColumns by columnsPreference
    val gridPadding = if (displayMode == LibraryDisplayMode.List) KatariTheme.sizes.none else KatariTheme.spacing.s
    val defaultCategoryTitle = stringResource(MR.strings.label_default)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            isListMode -> 1
            preferredColumns > 0 -> preferredColumns
            else -> adaptiveColumnCount(maxWidth - gridPadding * 2)
        }
        val gridState = rememberLazyGridState()
        BindScrollToTop(scrollToTopTarget, gridState) { gridState.animateScrollToItem(0) }
        FastScrollLazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            state = gridState,
            contentPadding = contentPadding + PaddingValues(gridPadding),
            // List rows are slices of one card per section, so no gap may open between them.
            verticalArrangement = Arrangement.spacedBy(
                if (displayMode == LibraryDisplayMode.List) {
                    KatariTheme.sizes.none
                } else {
                    CommonEntryItemDefaults.GridVerticalSpacer
                },
            ),
            horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "library_global_search_item") {
                GlobalSearchItem(
                    searchQuery = searchQuery,
                    onClick = onGlobalSearchClicked,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            sections.forEach { (page, items) ->
                val preview = searchResultPreview(
                    items = items,
                    columns = columns,
                    rows = if (isListMode) LIST_PREVIEW_ROWS else GRID_PREVIEW_ROWS,
                )
                item(
                    key = "search_header:${page.id}",
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = "library_search_header",
                ) {
                    LibrarySearchSectionHeader(
                        title = page.displayTitle(defaultCategoryTitle),
                        count = items.size,
                        onSeeAll = { onSeeAll(page) }.takeIf { preview.size < items.size },
                        overRowGroup = displayMode == LibraryDisplayMode.List,
                    )
                }
                val displaySettings = displaySettingsForPage(page)
                itemsIndexed(
                    items = preview,
                    key = { _, item -> "search_item:${page.id}:${item.key}" },
                    span = { _, item -> GridItemSpan(item.gridSpan(columns)) },
                    contentType = { _, _ -> "library_search_item" },
                ) { index, item ->
                    val onClick: (LibraryItem) -> Unit = { onClickItem(page, it) }
                    val onLongClick: (LibraryItem) -> Unit = { onLongClickItem(page, it) }
                    when (displayMode) {
                        LibraryDisplayMode.List -> RowGroupItem(
                            position = RowGroupPosition.of(index, preview.size),
                        ) {
                            LibraryListEntry(
                                libraryItem = item,
                                selection = selection,
                                onClick = onClick,
                                onLongClick = onLongClick,
                                onClickContinueReading = onClickContinueReading,
                                isContinueReadingAvailable = isContinueReadingAvailable,
                                displaySettings = displaySettings,
                            )
                        }
                        LibraryDisplayMode.CompactGrid, LibraryDisplayMode.CoverOnlyGrid -> LibraryCompactGridEntry(
                            libraryItem = item,
                            showTitle = displayMode is LibraryDisplayMode.CompactGrid,
                            selection = selection,
                            onClick = onClick,
                            onLongClick = onLongClick,
                            onClickContinueReading = onClickContinueReading,
                            isContinueReadingAvailable = isContinueReadingAvailable,
                            displaySettings = displaySettings,
                        )
                        LibraryDisplayMode.ComfortableGrid, LibraryDisplayMode.ComfortableList ->
                            LibraryComfortableGridEntry(
                                libraryItem = item,
                                selection = selection,
                                onClick = onClick,
                                onLongClick = onLongClick,
                                onClickContinueReading = onClickContinueReading,
                                isContinueReadingAvailable = isContinueReadingAvailable,
                                displaySettings = displaySettings,
                            )
                    }
                }
            }
        }
    }
}

/** Names the query that matched nothing and offers to look for it in sources instead. */
@Composable
private fun LibrarySearchNoResults(
    searchQuery: String,
    contentPadding: PaddingValues,
    onGlobalSearchClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyScreen(
        message = stringResource(MR.strings.library_search_no_match, searchQuery),
        modifier = modifier.padding(contentPadding),
    ) {
        Button(
            label = stringResource(MR.strings.library_search_sources_for, searchQuery),
            onClick = onGlobalSearchClicked,
            modifier = Modifier.padding(top = KatariTheme.spacing.xl),
            style = ButtonStyle.Tonal,
            icon = MaterialSymbols.Rounded.TravelExplore,
        )
        Text(
            text = stringResource(MR.strings.library_search_no_match_tip),
            modifier = Modifier.padding(top = KatariTheme.spacing.l),
            role = TextRole.Supporting,
            textAlign = TextAlign.Center,
        )
    }
}

/** A page's heading in the results. [overRowGroup] lines it up with the card of list rows under it. */
@Composable
private fun LibrarySearchSectionHeader(
    title: String,
    count: Int,
    onSeeAll: (() -> Unit)?,
    overRowGroup: Boolean,
) {
    val seeAll: (@Composable RowScope.() -> Unit)? = onSeeAll?.let {
        {
            Button(
                label = stringResource(MR.strings.library_search_see_all),
                onClick = it,
                style = ButtonStyle.Text,
            )
        }
    }
    if (overRowGroup) {
        RowGroupHeader(
            text = title,
            modifier = Modifier.semantics { heading() },
            trailing = { Text(text = count.toString(), role = TextRole.Supporting) },
            actions = seeAll,
        )
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { heading() }
            .padding(start = KatariTheme.spacing.s, top = KatariTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s),
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f, fill = false),
                role = TextRole.GroupLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = count.toString(),
                role = TextRole.Supporting,
            )
        }
        seeAll?.invoke(this)
    }
}

/** The first [rows] rows of a page's results, where a landscape cover takes two cells as it does on the page. */
private fun searchResultPreview(items: List<LibraryItem>, columns: Int, rows: Int): List<LibraryItem> {
    var remainingCells = columns * rows
    return items.takeWhile { item ->
        val span = item.gridSpan(columns)
        if (span > remainingCells) return@takeWhile false
        remainingCells -= span
        true
    }
}

private fun LibraryItem.gridSpan(columns: Int): Int {
    return if (sourceItemOrientation == EntryItemOrientation.HORIZONTAL) minOf(2, columns) else 1
}

private fun adaptiveColumnCount(availableWidth: Dp): Int {
    val spacing = CommonEntryItemDefaults.GridHorizontalSpacer
    return floor((availableWidth + spacing) / (BrowseMetrics.coverGridMinCellWidth + spacing)).toInt().coerceAtLeast(1)
}

private const val GRID_PREVIEW_ROWS = 2
private const val LIST_PREVIEW_ROWS = 4
