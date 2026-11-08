package mihon.book.api.document

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookDocumentTableTest {

    @Test
    fun `row spans carry occupied columns into later rows`() {
        val rows = listOf(
            row(cell("A", rowSpan = 2), cell("B")),
            row(cell("C")),
        )

        val layout = requireNotNull(rows.layoutBookDocumentTable())

        assertEquals(2, layout.columnCount)
        assertEquals(setOf(0), layout.rows[1].carriedColumns)
        assertEquals(1, layout.rows[1].placements.single().column)
    }

    @Test
    fun `grid rejects a row that cannot fit the bounded width`() {
        val rows = listOf(
            row(
                cell("A", columnSpan = MAX_BOOK_DOCUMENT_TABLE_CELL_SPAN),
                cell("B"),
            ),
        )

        assertNull(rows.layoutBookDocumentTable())
    }

    private fun row(vararg cells: BookDocumentTableCell) =
        BookDocumentTableRow(cells.toList())

    private fun cell(
        text: String,
        columnSpan: Int = 1,
        rowSpan: Int = 1,
    ) = BookDocumentTableCell(
        content = BookDocumentRichText(
            text = text,
            range = BookDocumentTextRange(0, text.length),
        ),
        header = false,
        scope = null,
        columnSpan = columnSpan,
        rowSpan = rowSpan,
    )
}
