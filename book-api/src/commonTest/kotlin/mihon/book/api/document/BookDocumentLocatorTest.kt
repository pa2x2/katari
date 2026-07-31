package mihon.book.api.document

import mihon.book.api.BookLocator
import mihon.book.api.BookTextContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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
    fun `anchor precedes block fragment and progression fallbacks`() {
        val text = "First\n\nSecond"
        val first = bookDocumentTextBlock("first", "First", logicalStart = 0, fragments = listOf("shared"))
        val second = bookDocumentTextBlock("second", "Second", logicalStart = 7, fragments = listOf("second"))
        val anchor = BookDocumentPosition(second.id, 3)
        val document = bookDocument(
            text = text,
            blocks = listOf(first, second),
            anchors = mapOf("shared" to anchor),
        )

        val restored = document.resolvePosition(
            BookLocator(
                resourceId = document.resourceId,
                progression = 0.0,
                fragments = listOf("shared"),
            ),
        )

        assertEquals(anchor, restored)
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

    @Test
    fun `progression-only locator restores through canonical coordinates`() {
        val document = duplicateParagraphDocument()

        val restored = requireNotNull(
            document.resolvePosition(
                BookLocator(
                    resourceId = document.resourceId,
                    progression = 0.75,
                ),
            ),
        )

        assertEquals(BookDocumentBlockId("second"), restored.blockId)
        assertEquals(8, restored.offsetWithinBlock)
    }

    @Test
    fun `locator for another resource is rejected`() {
        val document = duplicateParagraphDocument()

        assertNull(
            document.resolvePosition(
                BookLocator(
                    resourceId = "another-chapter",
                    progression = 0.5,
                ),
            ),
        )
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
