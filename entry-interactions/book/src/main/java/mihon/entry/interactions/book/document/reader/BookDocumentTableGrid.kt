package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentTableLayout

/** Content-sized columns and a common row grid, including cells spanning multiple rows. */
@Composable
internal fun BookDocumentTableGrid(
    grid: BookDocumentTableLayout,
    viewportWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val cells = grid.rows.flatMapIndexed { row, value -> value.placements.map { row to it } }
        val widths = IntArray(grid.columnCount) { 16.dp.roundToPx() }
        val minimumWidths = widths.copyOf()
        val availableWidth = viewportWidth.roundToPx().coerceAtLeast(1)
        cells.forEachIndexed { index, (_, placement) ->
            val cell = placement.cell
            fun distribute(destination: IntArray, desired: Int) {
                val current = (placement.column until placement.column + cell.columnSpan).sumOf { destination[it] }
                if (desired > current) {
                    val extra = (desired - current + cell.columnSpan - 1) / cell.columnSpan
                    repeat(cell.columnSpan) { destination[placement.column + it] += extra }
                }
            }
            distribute(
                minimumWidths,
                measurables[index].minIntrinsicWidth(Constraints.Infinity).coerceAtMost(availableWidth),
            )
            distribute(widths, measurables[index].maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(availableWidth))
        }
        widths.indices.forEach { widths[it] = maxOf(widths[it], minimumWidths[it]) }
        val preferredWidth = widths.sum()
        val minimumWidth = minimumWidths.sum()
        if (preferredWidth > availableWidth && preferredWidth > minimumWidth) {
            val remaining = (availableWidth - minimumWidth).coerceAtLeast(0)
            widths.indices.forEach { column ->
                val flexibleWidth = (widths[column] - minimumWidths[column]).toLong()
                widths[column] = minimumWidths[column] +
                    (flexibleWidth * remaining / (preferredWidth - minimumWidth)).toInt()
            }
        }
        val placeables = cells.mapIndexed { index, (_, placement) ->
            val width = (placement.column until placement.column + placement.cell.columnSpan).sumOf { widths[it] }
            measurables[index].measure(Constraints.fixedWidth(width))
        }
        val rowCount = cells.maxOf { (row, placement) -> row + placement.cell.rowSpan }
        val heights = IntArray(rowCount)
        // Single-row cells establish the base before spanning cells distribute any remaining height.
        cells.indices.sortedBy { cells[it].second.cell.rowSpan }.forEach { index ->
            val (row, placement) = cells[index]
            val span = placement.cell.rowSpan
            val current = (row until row + span).sumOf { heights[it] }
            val remaining = placeables[index].height - current
            if (remaining > 0) {
                repeat(span) { offset ->
                    heights[row + offset] +=
                        remaining / span + if (offset < remaining % span) 1 else 0
                }
            }
        }
        val x = widths.runningFold(0, Int::plus)
        val y = heights.runningFold(0, Int::plus)
        layout(constraints.constrainWidth(x.last()), constraints.constrainHeight(y.last())) {
            cells.forEachIndexed { index, (row, placement) ->
                placeables[index].placeRelative(x[placement.column], y[row])
            }
        }
    }
}
