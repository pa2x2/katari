package mihon.entry.interactions.book.prose

import eu.kanade.tachiyomi.source.entry.EntryType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mihon.book.api.BookLocator
import mihon.entry.interactions.book.OpenedBookReaderSession
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HtmlProseReaderSessionViewModelTest {
    @Test
    fun `switching chapters restores each retained locator`() {
        val first = session(1L, 1L, BookLocator("chapter-1", 0.2))
        val second = session(1L, 2L, BookLocator("chapter-2", 0.4))
        val model = HtmlProseReaderSessionViewModel()

        model.attachInitial(first)
        model.updateLocation(BookLocator("chapter-1", 0.7))
        assertTrue(model.cache(second))
        assertSame(second, model.switchTo(2L))
        assertEquals(0.4, model.currentLocator?.progression)
        assertSame(first, model.switchTo(1L))
        assertEquals(0.7, model.currentLocator?.progression)

        model.release()
    }

    @Test
    fun `retaining a new window closes sessions outside it`() {
        val first = session(1L, 1L)
        val second = session(1L, 2L)
        val third = session(1L, 3L)
        val model = HtmlProseReaderSessionViewModel()

        model.attachInitial(first)
        model.cache(second)
        model.cache(third)
        model.retain(setOf(2L))

        assertSame(first, model.currentSession)
        assertSame(second, model.cached(2L))
        verify(exactly = 1) { third.close() }

        model.release()
    }

    private fun session(
        entryId: Long,
        chapterId: Long,
        locator: BookLocator? = null,
    ): OpenedBookReaderSession = mockk(relaxed = true) {
        every { entry } returns Entry.create().copy(id = entryId, type = EntryType.BOOK)
        every { chapter } returns EntryChapter.create().copy(id = chapterId, entryId = entryId)
        every { initialLocator } returns locator
    }
}
