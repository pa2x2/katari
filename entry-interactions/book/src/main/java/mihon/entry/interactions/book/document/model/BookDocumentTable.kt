package mihon.entry.interactions.book.document.model

internal data class BookDocumentTableRow(
    val cells: List<BookDocumentTableCell>,
) {
    init {
        require(cells.isNotEmpty()) { "document table row must contain at least one cell" }
    }
}

internal data class BookDocumentTableCell(
    val text: String,
    val header: Boolean,
    val scope: BookDocumentTableCellScope?,
    val columnSpan: Int,
    val rowSpan: Int,
    val links: List<BookDocumentLink>,
) {
    init {
        require(columnSpan in 1..MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) {
            "document table column span is outside the supported range"
        }
        require(rowSpan in 1..MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN) {
            "document table row span is outside the supported range"
        }
        require(links.fitInside(text)) { "document table cell links must fit inside the cell text" }
    }
}

internal data class BookDocumentTableLayout(
    val columnCount: Int,
    val rows: List<BookDocumentTableLayoutRow>,
)

internal data class BookDocumentTableLayoutRow(
    val carriedColumns: Set<Int>,
    val placements: List<BookDocumentTableCellPlacement>,
)

internal data class BookDocumentTableCellPlacement(
    val column: Int,
    val cell: BookDocumentTableCell,
)

internal fun List<BookDocumentTableRow>.layoutBookDocumentTable(): BookDocumentTableLayout? {
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
                    nextCarried[occupiedColumn] = maxOf(nextCarried[occupiedColumn], cell.rowSpan - 1)
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

internal const val MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN = 24

internal enum class BookDocumentTableCellScope {
    ROW,
    COLUMN,
    ROW_GROUP,
    COLUMN_GROUP,
}
