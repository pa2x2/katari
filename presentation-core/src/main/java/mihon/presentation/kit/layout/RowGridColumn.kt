package mihon.presentation.kit.layout

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection

/**
 * Entries shown as rows by a lazy grid, such as the list display modes of the library or a catalogue, which share
 * the grid's items, headers and scroll state with its cover modes. [grid] lays out the grid with the [GridCells] and
 * content padding it is given: one column at most [RowListMaxWidth] wide and centred, as in a [RowList].
 */
@Composable
fun RowGridColumn(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    grid: @Composable (columns: GridCells, contentPadding: PaddingValues) -> Unit,
) {
    val direction = LocalLayoutDirection.current
    BoxWithConstraints(modifier = modifier, propagateMinConstraints = true) {
        grid(SingleColumn, contentPadding.centredInRowColumn(maxWidth, direction))
    }
}

private val SingleColumn = GridCells.Fixed(1)
