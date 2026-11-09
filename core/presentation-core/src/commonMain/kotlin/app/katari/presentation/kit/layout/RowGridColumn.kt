package app.katari.presentation.kit.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * Entries shown as rows by a lazy grid, such as the list display mode of library search results, which shares the
 * grid's items, headers and scroll state with its cover modes. [grid] lays out the grid with the [GridCells] and
 * content padding it is given: one column at most [RowListMaxWidth] wide, at the frame's start edge and clear of the
 * screen's side room, as in a [RowList]; [contentPadding] only adds to that room.
 */
@Composable
fun RowGridColumn(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    grid: @Composable (columns: GridCells, contentPadding: PaddingValues) -> Unit,
) {
    val direction = LocalLayoutDirection.current
    val padding = contentPadding.plusSides(LocalContentSides.current, direction)
    BoxWithConstraints(modifier = modifier, propagateMinConstraints = true) {
        SidesApplied { grid(SingleColumn, padding.inRowColumn(maxWidth, direction)) }
    }
}

private val SingleColumn = GridCells.Fixed(1)
