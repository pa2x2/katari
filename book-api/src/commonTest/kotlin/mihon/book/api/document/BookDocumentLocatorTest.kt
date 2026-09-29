package mihon.book.api.document

import mihon.book.api.BookLocator
import mihon.book.api.BookTextContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BookDocumentLocatorTest {

    @Test
    fun `precise locator restores duplicate text block and offset`() {
        val document = duplicateParagraphDocument()
        val position = BookDocumentPosition(BookDocumentBlockId("second"), 8)

        val locator = document.locatorAt(position)
        val restored = document.resolvePosition(locator)

        assertEquals(position, restored)
        assertEquals(listOf("second-fragment"), locator.fragments)
        assertNotNull(locator.textContext)
    }

    @Test
    fun `bounded text context disambiguates repeated text before progression`() {
        val text = "before target middle before target after"
        val block = bookDocumentTextBlock("only", text, logicalStart = 0)
        val document = bookDocument(text, listOf(block))
        val secondTarget = text.lastIndexOf("target")

        val restored = document.resolvePosition(
            BookLocator(
                resourceId = document.resourceId,
                progression = 0.0,
                textContext = BookTextContext(
                    before = "before ",
                    highlight = "target",
                    after = " after",
                ),
            ),
        )

        assertEquals(BookDocumentPosition(block.id, secondTarget), restored)
    }

    private fun duplicateParagraphDocument(): BookDocument {
        val first = bookDocumentTextBlock(
            id = "first",
            text = "Repeated paragraph",
            logicalStart = 0,
            fragments = listOf("first-fragment"),
        )
        val second = bookDocumentTextBlock(
            id = "second",
            text = "Repeated paragraph",
            logicalStart = 20,
            fragments = listOf("second-fragment"),
        )
        return bookDocument(
            text = "Repeated paragraph\n\nRepeated paragraph",
            blocks = listOf(first, second),
        )
    }
}
