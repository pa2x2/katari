package mihon.entry.interactions.book

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mihon.book.api.BookLocator
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class BookReaderSessionRegistryTest {
    @Test
    fun `session handoff can only be claimed once`() {
        val request = BookReaderRequest(entryId = 1L, chapterId = 2L)
        val session = session(request)
        val registry = BookReaderSessionRegistry()
        val token = registry.register(session)

        assertSame(session, registry.claim(token, request))
        assertNull(registry.claim(token, request))
    }

    @Test
    fun `mismatched handoff is closed instead of exposed`() {
        val request = BookReaderRequest(entryId = 1L, chapterId = 2L)
        val session = session(request)
        val registry = BookReaderSessionRegistry()
        val token = registry.register(session)

        assertNull(registry.claim(token, request.copy(chapterId = 3L)))
        verify(exactly = 1) { session.close() }
    }

    @Test
    fun `view model retains live locator and owns session lifetime`() {
        val request = BookReaderRequest(entryId = 1L, chapterId = 2L)
        val initial = BookLocator(resourceId = "one", progression = 0.25)
        val latest = BookLocator(resourceId = "two", progression = 0.75)
        val session = session(request, initial)
        val holder = BookReaderSessionViewModel()

        holder.attach(session)
        holder.updateLocation(latest)

        assertSame(session, holder.session)
        assertEquals(latest, holder.currentLocator)
        holder.release()
        verify(exactly = 1) { session.close() }
        assertNull(holder.session)
        assertNull(holder.currentLocator)
    }

    private fun session(
        request: BookReaderRequest,
        initialLocator: BookLocator? = null,
    ): OpenedBookReaderSession = mockk(relaxed = true) {
        every { entry } returns Entry.create().copy(id = request.entryId)
        every { chapter } returns EntryChapter.create().copy(id = request.chapterId)
        every { this@mockk.initialLocator } returns initialLocator
    }
}
