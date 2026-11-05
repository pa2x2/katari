package mihon.entry.interactions.book.document.reader.sidebyside

import mihon.book.api.document.BookDocumentGridTrack
import mihon.book.api.document.BookDocumentJustification
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BookDocumentSideBySideSizingTest {
    @Test
    fun `flex rows share free space by grow factor and wrap items that no longer fit`() {
        val grown = placeFlexItems(
            items = listOf(
                BookDocumentFlexItem(basis = 100, grow = 1f, shrink = 1f, minWidth = 0),
                BookDocumentFlexItem(basis = 50, grow = 3f, shrink = 1f, minWidth = 0),
                BookDocumentFlexItem(basis = 40, grow = 0f, shrink = 1f, minWidth = 0),
            ),
            available = 300,
            gap = 10,
            wrap = false,
            reversed = false,
            justify = BookDocumentJustification.START,
        )
        // 90 px free: a quarter to the first item, three quarters to the second.
        assertEquals(
            listOf(
                BookDocumentItemPlacement(0, 0, 123),
                BookDocumentItemPlacement(0, 133, 118),
                BookDocumentItemPlacement(0, 261, 40),
            ),
            grown,
        )

        val wrapped = placeFlexItems(
            items = listOf(120, 120, 60).map { BookDocumentFlexItem(basis = it, grow = 0f, shrink = 1f, minWidth = 0) },
            available = 200,
            gap = 10,
            wrap = true,
            reversed = false,
            justify = BookDocumentJustification.SPACE_BETWEEN,
        )
        assertEquals(
            listOf(
                BookDocumentItemPlacement(0, 0, 120),
                BookDocumentItemPlacement(1, 0, 120),
                BookDocumentItemPlacement(1, 140, 60),
            ),
            wrapped,
        )
    }

    @Test
    fun `grid columns size fixed then fr tracks, and items place row by row around explicit columns and spans`() {
        val placements = placeGridItems(
            items = listOf(
                BookDocumentGridItem(column = null, span = 1, contentWidth = 0),
                BookDocumentGridItem(column = 2, span = 1, contentWidth = 0),
                BookDocumentGridItem(column = null, span = 2, contentWidth = 0),
                BookDocumentGridItem(column = null, span = 1, contentWidth = 0),
            ),
            tracks = listOf(
                BookDocumentGridTrack.Fixed(5f),
                BookDocumentGridTrack.Flexible(1f),
                BookDocumentGridTrack.Flexible(2f),
            ),
            autoFillMin = null,
            emPx = 16f,
            available = 400,
            gap = 20,
        )
        // 360 px between the gaps: 80 fixed, then 280 split one to two.
        assertEquals(
            listOf(
                BookDocumentItemPlacement(0, 0, 80),
                BookDocumentItemPlacement(0, 213, 187),
                BookDocumentItemPlacement(1, 0, 193),
                BookDocumentItemPlacement(1, 213, 187),
            ),
            placements,
        )

        val filled = placeGridItems(
            items = List(4) { BookDocumentGridItem(column = null, span = 1, contentWidth = 0) },
            tracks = emptyList(),
            autoFillMin = 120,
            emPx = 16f,
            available = 400,
            gap = 20,
        )
        assertEquals(listOf(0, 0, 0, 1), filled.map { it.line })
        assertEquals(listOf(120, 120, 120, 120), filled.map { it.width })
    }
}
