package mihon.entry.interactions.book.epub

import mihon.book.api.BookLocator
import mihon.book.api.BookResource
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadiumEpubCompletionTest {
    private val readingOrder = listOf(
        BookResource("chapter-1.xhtml", "application/xhtml+xml", "Chapter 1"),
        BookResource("chapter-2.xhtml", "application/xhtml+xml", "Chapter 2"),
    )

    @Test
    fun `publication completion follows total progression when available`() {
        assertFalse(
            isEpubPublicationComplete(
                BookLocator("chapter-2.xhtml", progression = 1.0, totalProgression = 0.994),
                readingOrder,
            ),
        )
        assertTrue(
            isEpubPublicationComplete(
                BookLocator("chapter-2.xhtml", progression = 0.5, totalProgression = 0.995),
                readingOrder,
            ),
        )
    }

    @Test
    fun `publication completion falls back to the end of the final resource`() {
        assertFalse(
            isEpubPublicationComplete(
                BookLocator("chapter-1.xhtml", progression = 1.0),
                readingOrder,
            ),
        )
        assertFalse(
            isEpubPublicationComplete(
                BookLocator("chapter-2.xhtml", progression = 0.994),
                readingOrder,
            ),
        )
        assertTrue(
            isEpubPublicationComplete(
                BookLocator("chapter-2.xhtml", progression = 0.995),
                readingOrder,
            ),
        )
    }

    @Test
    fun `publication without a reading order does not infer completion`() {
        assertFalse(
            isEpubPublicationComplete(
                BookLocator("chapter-1.xhtml", progression = 1.0),
                emptyList(),
            ),
        )
    }
}
