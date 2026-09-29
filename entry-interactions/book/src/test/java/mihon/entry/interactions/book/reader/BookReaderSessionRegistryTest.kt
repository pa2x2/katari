package mihon.entry.interactions.book.reader

import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.book.processor.BookReaderRequest
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
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

    private fun session(
        request: BookReaderRequest,
    ): OpenedBookReaderSession = mockk(relaxed = true) {
        every { entry } returns Entry.create().copy(id = request.entryId)
        every { chapter } returns EntryChapter.create().copy(id = request.chapterId)
    }
}
