package mihon.entry.interactions.book.document.reader.navigation

import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BookDocumentJumpHistoryTest {
    @Test
    fun `each jump remembers the latest passage while ordinary reading preserves the return point`() {
        val first = seekSection()
        val second = seekSection("other", 2)
        val position = first.document.document.positionAtProgression(.37f)
        val history = BookDocumentJumpHistory()
        history.observe(BookDocumentViewerLocation(first, position, .37f))
        history.rememberOrigin()
        val firstTarget = requireNotNull(history.returnTarget)
        assertEquals(first.owner.id, firstTarget.chapter.id)
        assertEquals(position, first.document.document.resolvePosition(requireNotNull(firstTarget.locator)))

        val secondPosition = second.document.document.positionAtProgression(.6f)
        history.observe(BookDocumentViewerLocation(second, secondPosition, .6f))
        assertEquals(firstTarget, history.returnTarget)
        history.rememberOrigin()
        val target = requireNotNull(history.returnTarget)
        assertEquals(second.owner.id, target.chapter.id)
        assertEquals(secondPosition, second.document.document.resolvePosition(requireNotNull(target.locator)))
        history.observe(BookDocumentViewerLocation(first, first.initialPosition, 0f))
        assertEquals(target, history.returnTarget)
        history.dismiss()
        assertNull(history.returnTarget)
        history.rememberOrigin()
        assertEquals(first.owner.id, history.returnTarget?.chapter?.id)
    }
}
