package eu.kanade.tachiyomi.ui.browse.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.kanade.presentation.library.components.CommonEntryItemDefaults
import eu.kanade.tachiyomi.ui.browse.feed.timeline.FeedRow
import mihon.presentation.kit.list.RowGroupItem
import mihon.presentation.kit.list.RowGroupPosition
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.presentation.core.util.plus

/**
 * A feed's rows one per line, with [footer] after them when [showFooter]. Each run of entries between gap and divider
 * rows is one row group card.
 */
@Composable
internal fun FeedLazyList(
    rows: List<FeedRow>,
    listState: LazyListState,
    contentPadding: PaddingValues,
    showFooter: Boolean,
    rowContent: @Composable (FeedRow) -> Unit,
    footer: @Composable () -> Unit,
) {
    val positions = remember(rows) { RowGroupPosition.ofRuns(rows) { it is FeedRow.Item } }
    LazyColumn(
        state = listState,
        contentPadding = contentPadding + PaddingValues(vertical = KatariTheme.spacing.s),
    ) {
        itemsIndexed(
            items = rows,
            key = { _, row -> row.key },
            contentType = { _, row -> row.contentType() },
        ) { index, row ->
            val position = positions[index]
            if (position != null) {
                RowGroupItem(position = position) { rowContent(row) }
            } else {
                rowContent(row)
            }
        }
        if (showFooter) {
            item(key = FOOTER_KEY) { footer() }
        }
    }
}

/**
 * A feed's rows in a grid. Gap and divider rows take a whole line, so the line before one ends early: a seen item
 * never shares a line with new ones.
 */
@Composable
internal fun FeedLazyGrid(
    rows: List<FeedRow>,
    columns: GridCells,
    gridState: LazyGridState,
    contentPadding: PaddingValues,
    showFooter: Boolean,
    rowContent: @Composable (FeedRow) -> Unit,
    footer: @Composable () -> Unit,
) {
    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        contentPadding = contentPadding + PaddingValues(KatariTheme.spacing.s),
        verticalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridVerticalSpacer),
        horizontalArrangement = Arrangement.spacedBy(CommonEntryItemDefaults.GridHorizontalSpacer),
    ) {
        items(
            items = rows,
            key = FeedRow::key,
            span = { row -> if (row is FeedRow.Item) GridItemSpan(1) else GridItemSpan(maxLineSpan) },
            contentType = FeedRow::contentType,
        ) { row ->
            rowContent(row)
        }
        if (showFooter) {
            item(key = FOOTER_KEY, span = { GridItemSpan(maxLineSpan) }) { footer() }
        }
    }
}

private fun FeedRow.contentType(): Int {
    return when (this) {
        is FeedRow.Item -> 0
        is FeedRow.Gap -> 1
        FeedRow.SeenDivider -> 2
    }
}

private const val FOOTER_KEY = "feed-footer"
