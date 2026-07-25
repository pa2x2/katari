package mihon.entry.interactions.book.document.location

import android.text.SpannableString
import mihon.book.api.BookLocator
import mihon.entry.interactions.book.document.model.BookDocument
import mihon.entry.interactions.book.document.model.BookDocumentBlock
import mihon.entry.interactions.book.document.model.BookDocumentBlockId
import mihon.entry.interactions.book.document.model.BookDocumentBlockKind
import mihon.entry.interactions.book.document.model.BookDocumentBlockRole
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class BookDocumentLocatorTest {
    @Test
    fun `precise locator restores the selected duplicate block and offset`() {
        val document = document()
        val position = BookDocumentPosition(BookDocumentBlockId("second"), 8)

        val locator = document.locatorAt(position)
        val restored = document.resolvePosition(locator)

        assertEquals(position, restored)
        assertEquals(listOf("second-fragment"), locator.fragments)
        assertNotNull(locator.textContext)
    }

    @Test
    fun `legacy progression-only locator restores through logical document coordinates`() {
        val document = document()
        val legacy = BookLocator(
            resourceId = document.document.resourceId,
            progression = 0.75,
        )

        val restored = requireNotNull(document.resolvePosition(legacy))

        assertEquals(BookDocumentBlockId("second"), restored.blockId)
        assertEquals(8, restored.offsetWithinBlock)
    }

    private fun document(): PreparedBookDocument {
        val first = block("first", "first-fragment", "Repeated paragraph", 0)
        val second = block("second", "second-fragment", "Repeated paragraph", 20)
        val model = BookDocument(
            resourceId = "chapter",
            revision = "r1",
            blocks = listOf(first, second),
            anchors = emptyMap(),
            logicalExtent = 38,
        )
        return PreparedBookDocument(
            document = model,
            blocks = listOf(first, second).map {
                PreparedBookDocumentBlock(it, SpannableString(it.plainText))
            },
            combinedText = SpannableString("Repeated paragraph\n\nRepeated paragraph"),
        )
    }

    private fun block(
        id: String,
        fragment: String,
        text: String,
        start: Int,
    ) = BookDocumentBlock(
        id = BookDocumentBlockId(id),
        role = BookDocumentBlockRole(BookDocumentBlockKind.PARAGRAPH),
        plainText = text,
        sourceFragments = listOf(fragment),
        logicalStart = start,
        logicalEndExclusive = start + 18,
    )
}
