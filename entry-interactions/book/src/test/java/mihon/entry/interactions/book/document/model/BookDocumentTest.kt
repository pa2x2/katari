package mihon.entry.interactions.book.document.model

import org.junit.Test
import kotlin.test.assertEquals

class BookDocumentTest {
    @Test
    fun `logical offset preserves an exact block boundary`() {
        val first = block(id = "first", start = 0, end = 10)
        val second = block(id = "second", start = 10, end = 20)
        val document = BookDocument(
            resourceId = "resource",
            revision = "revision",
            blocks = listOf(first, second),
            anchors = emptyMap(),
            logicalExtent = 20,
        )

        assertEquals(
            BookDocumentPosition(second.id, 0),
            document.positionAtLogicalOffset(10),
        )
        assertEquals(
            BookDocumentPosition(second.id, second.logicalLength),
            document.positionAtLogicalOffset(20),
        )
    }

    private fun block(
        id: String,
        start: Int,
        end: Int,
    ) = BookDocumentBlock(
        id = BookDocumentBlockId(id),
        role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
        plainText = id,
        sourceFragments = emptyList(),
        logicalStart = start,
        logicalEndExclusive = end,
    )
}
