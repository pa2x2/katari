package app.katari.presentation.kit.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import tachiyomi.presentation.core.components.FastScrollLazyVerticalGrid

/**
 * A scrolling grid of entries, such as a library page, a catalog or a feed, in the [columns] of [coverGridCells],
 * [coverListCells] or [RowGridColumn]. It keeps clear of the screen's side room itself, so [contentPadding] only adds
 * to it, such as the room under the app bar. A window worked with a mouse shows a scrollbar at its end edge;
 * [fastScroll] gives a touch window a draggable thumb for long grids. [content] is given how many columns the grid lays
 * out, for items that size themselves by it, such as a preview that fills whole lines.
 */
@Composable
fun CoverGrid(
    columns: GridCells,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    fastScroll: Boolean = false,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    content: LazyGridScope.(columnCount: Int) -> Unit,
) {
    val direction = LocalLayoutDirection.current
    val padding = contentPadding.plusSides(LocalContentSides.current, direction)
    BoxWithConstraints(modifier = modifier, propagateMinConstraints = true) {
        val width = maxWidth - padding.calculateStartPadding(direction) - padding.calculateEndPadding(direction)
        val columnCount = columns.columnCount(width, horizontalArrangement.spacing)
        val gridContent: LazyGridScope.() -> Unit = { content(columnCount) }
        SidesApplied {
            Grid(columns, state, padding, fastScroll, verticalArrangement, horizontalArrangement, gridContent)
        }
    }
}

@Composable
private fun Grid(
    columns: GridCells,
    state: LazyGridState,
    padding: PaddingValues,
    fastScroll: Boolean,
    verticalArrangement: Arrangement.Vertical,
    horizontalArrangement: Arrangement.Horizontal,
    content: LazyGridScope.() -> Unit,
) {
    if (fastScroll) {
        FastScrollLazyVerticalGrid(
            columns = columns,
            state = state,
            contentPadding = padding,
            verticalArrangement = verticalArrangement,
            horizontalArrangement = horizontalArrangement,
            content = content,
        )
    } else {
        ScrollbarFrame(state = state, contentPadding = padding) {
            LazyVerticalGrid(
                columns = columns,
                state = state,
                contentPadding = padding,
                verticalArrangement = verticalArrangement,
                horizontalArrangement = horizontalArrangement,
                content = content,
            )
        }
    }
}
