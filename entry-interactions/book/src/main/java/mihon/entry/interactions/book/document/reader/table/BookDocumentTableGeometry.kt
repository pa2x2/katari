package mihon.entry.interactions.book.document.reader.table

import androidx.compose.ui.unit.IntRect
import mihon.book.api.document.BookDocumentTableLayout
import kotlin.math.ceil

internal data class BookDocumentTableGeometry(
    val cells: List<IntRect>,
    val width: Int,
    val height: Int,
)

/** Measures text without composing offscreen selection, link, or layout nodes. */
internal fun measureBookDocumentTable(
    grid: BookDocumentTableLayout,
    texts: List<BookDocumentTableCellText>,
    measurer: BookDocumentTableTextMeasurer,
    availableWidth: Int,
    cellPadding: Int,
): BookDocumentTableGeometry {
    val cells = grid.rows.flatMapIndexed { row, value -> value.placements.map { row to it } }
    val widths = IntArray(grid.columnCount) { cellPadding * 2 }
    val minimumWidths = widths.copyOf()
    texts.forEachIndexed { index, text ->
        val intrinsics = measurer.intrinsics(text)
        val placement = cells[index].second
        fun distribute(destination: IntArray, desired: Int) {
            val current = (placement.column until placement.column + placement.cell.columnSpan).sumOf {
                destination[it]
            }
            if (desired > current) {
                val extra = (desired - current + placement.cell.columnSpan - 1) / placement.cell.columnSpan
                repeat(placement.cell.columnSpan) { destination[placement.column + it] += extra }
            }
        }
        distribute(
            minimumWidths,
            (ceil(intrinsics.minIntrinsicWidth).toInt() + cellPadding * 2).coerceAtMost(availableWidth),
        )
        distribute(widths, (ceil(intrinsics.maxIntrinsicWidth).toInt() + cellPadding * 2).coerceAtMost(availableWidth))
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
    val cellHeights = texts.mapIndexed { index, text ->
        val placement = cells[index].second
        val width = (placement.column until placement.column + placement.cell.columnSpan).sumOf { widths[it] }
        measurer.height(text, (width - cellPadding * 2).coerceAtLeast(0)) + cellPadding * 2 + text.bottomSpacing
    }
    val heights = IntArray(cells.maxOf { (row, placement) -> row + placement.cell.rowSpan })
    cells.indices.sortedBy { cells[it].second.cell.rowSpan }.forEach { index ->
        val (row, placement) = cells[index]
        val span = placement.cell.rowSpan
        val remaining = cellHeights[index] - (row until row + span).sumOf { heights[it] }
        if (remaining > 0) {
            repeat(span) { offset ->
                heights[row + offset] += remaining / span + if (offset < remaining % span) 1 else 0
            }
        }
    }
    val x = widths.runningFold(0, Int::plus)
    val y = heights.runningFold(0, Int::plus)
    return BookDocumentTableGeometry(
        cells = cells.map { (row, placement) ->
            IntRect(
                x[placement.column],
                y[row],
                x[placement.column + placement.cell.columnSpan],
                y[row + placement.cell.rowSpan],
            )
        },
        width = x.last(),
        height = y.last(),
    )
}
