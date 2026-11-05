package mihon.entry.interactions.book.document.reader.sidebyside

import mihon.book.api.document.BookDocumentGridTrack
import mihon.book.api.document.BookDocumentJustification
import kotlin.math.roundToInt

/*
 * Where the items of a side-by-side row go, in pixels: a simplified CSS flex and grid layout without the reader's
 * composables, so it can be checked against widths worked out by hand.
 */

/** An item's spot: the line (flex line or grid row) it sits on, its start offset in the row and its width. */
internal data class BookDocumentItemPlacement(val line: Int, val x: Int, val width: Int)

/**
 * A flex item. [basis] is its preferred width; [minWidth] is the narrowest it gets before its content overflows,
 * which CSS's `min-width: auto` gives flex items.
 */
internal data class BookDocumentFlexItem(val basis: Int, val grow: Float, val shrink: Float, val minWidth: Int)

internal fun placeFlexItems(
    items: List<BookDocumentFlexItem>,
    available: Int,
    gap: Int,
    wrap: Boolean,
    reversed: Boolean,
    justify: BookDocumentJustification,
): List<BookDocumentItemPlacement> {
    val lines = mutableListOf<MutableList<Int>>()
    var used = 0
    items.forEachIndexed { index, item ->
        val size = maxOf(item.basis, item.minWidth)
        val current = lines.lastOrNull()
        if (current == null || (wrap && current.isNotEmpty() && used + gap + size > available)) {
            lines += mutableListOf(index)
            used = size
        } else {
            current += index
            used += gap + size
        }
    }
    val placements = arrayOfNulls<BookDocumentItemPlacement>(items.size)
    lines.forEachIndexed { lineIndex, line ->
        val widths = flexWidths(line.map(items::get), available - gap * (line.size - 1))
        val free = (available - gap * (line.size - 1) - widths.sum()).coerceAtLeast(0)
        val (start, spacing) = when (justify) {
            BookDocumentJustification.START -> 0f to 0f
            BookDocumentJustification.CENTER -> free / 2f to 0f
            BookDocumentJustification.END -> free.toFloat() to 0f
            BookDocumentJustification.SPACE_BETWEEN ->
                if (line.size > 1) 0f to free.toFloat() / (line.size - 1) else 0f to 0f
            BookDocumentJustification.SPACE_AROUND -> free / (line.size * 2f) to free.toFloat() / line.size
            BookDocumentJustification.SPACE_EVENLY -> free / (line.size + 1f) to free / (line.size + 1f)
        }
        var x = start
        line.forEachIndexed { position, index ->
            val width = widths[position]
            val offset = x.roundToInt()
            placements[index] = BookDocumentItemPlacement(
                line = lineIndex,
                x = if (reversed) available - offset - width else offset,
                width = width,
            )
            x += width + gap + spacing
        }
    }
    return placements.map(::checkNotNull)
}

// Free space goes to items by their grow factors; missing space is taken by shrink factor times basis, as CSS does,
// down to each item's minimum. When even the minimums don't fit, every item shrinks in proportion.
private fun flexWidths(line: List<BookDocumentFlexItem>, space: Int): List<Int> {
    val bases = line.map { maxOf(it.basis, it.minWidth).toFloat() }
    val free = space - bases.sum()
    val widths = if (free >= 0f) {
        val grow = line.sumOf { it.grow.toDouble() }.toFloat()
        if (grow > 0f) bases.mapIndexed { index, basis -> basis + free * line[index].grow / grow } else bases
    } else {
        val weights = line.mapIndexed { index, item -> item.shrink * bases[index] }
        val total = weights.sum()
        if (total > 0f) {
            bases.mapIndexed { index, basis ->
                maxOf(line[index].minWidth.toFloat(), basis + free * weights[index] / total)
            }
        } else {
            bases
        }
    }
    val sum = widths.sum()
    val fitted = if (sum > space && sum > 0f) widths.map { it * space / sum } else widths
    return fitted.map { it.roundToInt().coerceAtLeast(0) }
}

/** A grid item: its explicit column if any, the columns it spans and the width its content wants. */
internal data class BookDocumentGridItem(val column: Int?, val span: Int, val contentWidth: Int)

/**
 * Places [items] row by row into columns sized by [tracks], or into as many equal columns of at least
 * [autoFillMin] pixels as fit. Lengths in [tracks] are converted with [emPx].
 */
internal fun placeGridItems(
    items: List<BookDocumentGridItem>,
    tracks: List<BookDocumentGridTrack>,
    autoFillMin: Int?,
    emPx: Float,
    available: Int,
    gap: Int,
): List<BookDocumentItemPlacement> {
    val columnCount = if (autoFillMin != null) {
        ((available + gap) / (autoFillMin + gap).coerceAtLeast(1)).coerceAtLeast(1)
    } else {
        tracks.size
    }
    val cells = autoPlace(items, columnCount)
    val widths = if (autoFillMin != null) {
        List(columnCount) { (available - gap * (columnCount - 1)).coerceAtLeast(0) / columnCount }
    } else {
        trackWidths(tracks, items, cells, emPx, available, gap)
    }
    val starts = widths.runningFold(0) { x, width -> x + width + gap }
    return items.indices.map { index ->
        val (row, column, span) = cells[index]
        val width = widths.subList(column, column + span).sum() + gap * (span - 1)
        BookDocumentItemPlacement(line = row, x = starts[column], width = width)
    }
}

private data class GridCell(val row: Int, val column: Int, val span: Int)

// CSS's sparse auto-placement: a cursor walks the rows; an item with an explicit column moves the cursor to it,
// on the next row when that column is already behind.
private fun autoPlace(items: List<BookDocumentGridItem>, columnCount: Int): List<GridCell> {
    var row = 0
    var column = 0
    return items.map { item ->
        val span = item.span.coerceIn(1, columnCount)
        val wanted = item.column?.coerceIn(0, columnCount - span)
        if (wanted != null) {
            if (wanted < column) row++
            column = wanted
        } else if (column + span > columnCount) {
            row++
            column = 0
        }
        GridCell(row, column, span).also {
            column += span
            if (column >= columnCount) {
                row++
                column = 0
            }
        }
    }
}

private fun trackWidths(
    tracks: List<BookDocumentGridTrack>,
    items: List<BookDocumentGridItem>,
    cells: List<GridCell>,
    emPx: Float,
    available: Int,
    gap: Int,
): List<Int> {
    val space = (available - gap * (tracks.size - 1)).coerceAtLeast(0).toFloat()
    val sized = tracks.mapIndexed { column, track ->
        when (track) {
            is BookDocumentGridTrack.Fixed -> track.em * emPx
            is BookDocumentGridTrack.OfRow -> track.fraction * space
            BookDocumentGridTrack.Auto ->
                items.indices
                    .filter { cells[it].column == column && cells[it].span == 1 }
                    .maxOfOrNull { items[it].contentWidth.toFloat() } ?: 0f
            is BookDocumentGridTrack.Flexible -> 0f
        }
    }
    val shares = tracks.map { (it as? BookDocumentGridTrack.Flexible)?.share ?: 0f }
    val totalShare = shares.sum()
    val leftover = (space - sized.sum()).coerceAtLeast(0f)
    val widths = sized.mapIndexed { column, width ->
        if (totalShare > 0f) width + leftover * shares[column] / totalShare else width
    }
    val sum = widths.sum()
    val fitted = if (sum > space && sum > 0f) widths.map { it * space / sum } else widths
    return fitted.map { it.roundToInt().coerceAtLeast(0) }
}
