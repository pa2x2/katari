package mihon.book.api.document

import kotlinx.serialization.Serializable

/**
 * One semantic table row.
 *
 * @property cells cells in source order.
 */
@Serializable
data class BookDocumentTableRow(
    val cells: List<BookDocumentTableCell>,
) {
    init {
        require(cells.isNotEmpty()) { "document table row must contain at least one cell" }
    }
}

/**
 * One semantic table cell.
 *
 * @property content rich cell text mapped into the owning table block.
 * @property header whether the cell is a header.
 * @property scope optional accessibility header scope.
 * @property columnSpan bounded column span.
 * @property rowSpan bounded row span.
 */
@Serializable
data class BookDocumentTableCell(
    val content: BookDocumentRichText,
    val header: Boolean,
    val scope: BookDocumentTableCellScope?,
    val columnSpan: Int,
    val rowSpan: Int,
) {
    init {
        require(columnSpan in 1..MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) {
            "document table column span is outside the supported range"
        }
        require(rowSpan in 1..MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) {
            "document table row span is outside the supported range"
        }
    }

    /** Exact cell text. */
    val text: String
        get() = content.text

    /** Cell-relative semantic links. */
    val links: List<BookDocumentLink>
        get() = content.links
}

/**
 * Rowspan-aware table grid.
 *
 * @property columnCount total grid width.
 * @property rows laid-out rows.
 */
@Serializable
data class BookDocumentTableLayout(
    val columnCount: Int,
    val rows: List<BookDocumentTableLayoutRow>,
)

/**
 * One laid-out table row.
 *
 * @property carriedColumns columns occupied by a preceding rowspan.
 * @property placements source cells placed in this row.
 */
@Serializable
data class BookDocumentTableLayoutRow(
    val carriedColumns: Set<Int>,
    val placements: List<BookDocumentTableCellPlacement>,
)

/**
 * Grid placement of one source table cell.
 *
 * @property column zero-based starting column.
 * @property cell source semantic cell.
 */
@Serializable
data class BookDocumentTableCellPlacement(
    val column: Int,
    val cell: BookDocumentTableCell,
)

/**
 * Produces a bounded rowspan-aware grid for semantic table rows.
 *
 * @return validated layout, or `null` when cells cannot fit within the supported grid.
 */
fun List<BookDocumentTableRow>.layoutBookDocumentTable(): BookDocumentTableLayout? {
    if (isEmpty()) return null
    var carried = IntArray(MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN)
    val laidOutRows = mutableListOf<BookDocumentTableLayoutRow>()
    var columnCount = 0
    for (row in this) {
        val carriedColumns = carried.indices.filterTo(linkedSetOf()) { carried[it] > 0 }
        val occupied = BooleanArray(MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) { it in carriedColumns }
        val nextCarried = IntArray(MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) { column ->
            (carried[column] - 1).coerceAtLeast(0)
        }
        val placements = mutableListOf<BookDocumentTableCellPlacement>()
        for (cell in row.cells) {
            val column = occupied.firstAvailableRange(cell.columnSpan) ?: return null
            placements += BookDocumentTableCellPlacement(column, cell)
            repeat(cell.columnSpan) { offset ->
                val occupiedColumn = column + offset
                occupied[occupiedColumn] = true
                if (cell.rowSpan > 1) {
                    nextCarried[occupiedColumn] = maxOf(
                        nextCarried[occupiedColumn],
                        cell.rowSpan - 1,
                    )
                }
            }
            columnCount = maxOf(columnCount, column + cell.columnSpan)
        }
        laidOutRows += BookDocumentTableLayoutRow(carriedColumns, placements)
        carried = nextCarried
    }
    return BookDocumentTableLayout(columnCount, laidOutRows)
}

private fun BooleanArray.firstAvailableRange(width: Int): Int? {
    for (start in 0..size - width) {
        if ((start until start + width).none { this[it] }) return start
    }
    return null
}

/** Maximum supported table width and individual cell span. */
const val MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN = 24

/** Accessibility scope of a semantic table header. */
@Serializable
enum class BookDocumentTableCellScope {
    ROW,
    COLUMN,
    ROW_GROUP,
    COLUMN_GROUP,
}
