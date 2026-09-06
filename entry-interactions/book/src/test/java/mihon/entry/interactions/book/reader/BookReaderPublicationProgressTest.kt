package mihon.entry.interactions.book.reader

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookLocator
import mihon.entry.interactions.book.document.preparation.preparedDocumentPublication
import mihon.entry.interactions.book.media.session.BookMediaSessionProcessor
import mihon.entry.interactions.book.preparation.PreparedBookPublication
import mihon.entry.interactions.book.state.BookProgressIdentity
import mihon.entry.interactions.book.state.BookProgressLocatorCodec
import mihon.entry.interactions.media.session.EntryMediaSessionEvent
import mihon.entry.interactions.media.session.EntryMediaSessionEventSink
import mihon.entry.interactions.media.session.EntryMediaSessionResult
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class BookReaderPublicationProgressTest : BookReaderSessionFixture() {
    @Test
    fun `multi document progress reports the publication while retaining the exact resource location`() = runTest {
        val publication = preparedDocumentPublication("one" to "<p>abcdefghij</p>", "two" to "<p>abcdefghij</p>")
        val location = BookLocator("two", progression = 0.5, totalProgression = 0.99, fragments = listOf("target"))
        val event = save(publication, location)

        assertEquals(0.75, event.fraction)
        assertEquals(0.75, event.progress.locator.progression)
        assertEquals(location.copy(totalProgression = 0.75), BookProgressLocatorCodec.decode(event.progress.locator))
    }

    @Test
    fun `chapter progress does not depend on its position in the catalogue`() = runTest {
        val publication = preparedDocumentPublication("one" to "<p>abcdefghij</p>")
        val event = save(publication, BookLocator("one", progression = 0.1, totalProgression = 0.99))

        assertEquals(0.1, requireNotNull(event.fraction), 1e-9)
        assertEquals(0.1, requireNotNull(event.progress.locator.progression), 1e-9)
    }

    private suspend fun save(
        publication: PreparedBookPublication,
        locator: BookLocator,
    ): EntryMediaSessionEvent.Progressed {
        val events = mutableListOf<EntryMediaSessionEvent>()
        val session = OpenedBookReaderSession(
            entry = entry(),
            owner = entry(),
            chapter = chapter(),
            progressIdentity = BookProgressIdentity("", "book", null),
            contentSession = mockk(relaxed = true),
            preparedPublication = publication,
            initialLocator = null,
            mediaSession = BookMediaSessionProcessor(
                EntryMediaSessionEventSink {
                    events += it
                    EntryMediaSessionResult.Handled
                },
            ),
            now = { 100L },
        )
        session.saveLocation(locator)
        session.close()
        return assertIs<EntryMediaSessionEvent.Progressed>(events.single())
    }
}
