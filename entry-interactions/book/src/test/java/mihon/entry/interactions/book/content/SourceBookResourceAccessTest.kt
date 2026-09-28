package mihon.entry.interactions.book.content

import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.EntryMedia
import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookResourceAvailability
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
internal class SourceBookResourceAccessTest : SourceBookContentSessionFixture() {
    @Test
    fun `source child resolves through existing getMedia API and keeps stable resource identity`() = runTest {
        val source = source()
        coEvery { source.getMedia(match { it.url == "/chapter/1" }, any()) } returns EntryMedia.Book(
            descriptor = BookContentDescriptor("text/html"),
            initialResourceId = "chapter-1",
            initialResourceLocation = BookResourceLocation.InlineText("Resolved chapter", "text/html"),
        )
        val session = session(
            source = source,
            media = bookMedia(
                resources = listOf(
                    resource(
                        "chapter-1",
                        location = BookResourceLocation.SourceChild("chapter-1", "/chapter/1"),
                    ),
                ),
            ),
        )

        session.openResource("chapter-1").getOrThrow().use { opened ->
            assertEquals("chapter-1", opened.metadata.id)
            assertEquals("Resolved chapter", opened.stream.bufferedReader().readText())
        }
    }

    @Test
    fun `source child loops and mismatched media fail without recursion`() = runTest {
        val loopingSource = source()
        coEvery { loopingSource.getMedia(any(), any()) } returns EntryMedia.Book(
            descriptor = BookContentDescriptor("text/html"),
            initialResourceId = "chapter-1",
            initialResourceLocation = BookResourceLocation.SourceChild("chapter-1", "/chapter/1"),
        )
        val loopSession = session(
            source = loopingSource,
            media = bookMedia(
                resources = listOf(
                    resource(
                        "chapter-1",
                        location = BookResourceLocation.SourceChild("chapter-1", "/chapter/1"),
                    ),
                ),
            ),
        )

        val loopFailure = assertNotNull(loopSession.openResource("chapter-1").exceptionOrNull())
        assertTrue(loopFailure.message.orEmpty().contains("loop"))

        val mismatchedSource = source()
        coEvery { mismatchedSource.getMedia(any(), any()) } returns EntryMedia.ImagePages(emptyList())
        val mismatchSession = session(
            source = mismatchedSource,
            media = bookMedia(
                resources = listOf(
                    resource(
                        "chapter-1",
                        location = BookResourceLocation.SourceChild("chapter-1", "/chapter/1"),
                    ),
                ),
            ),
        )

        val mismatchFailure = assertNotNull(mismatchSession.openResource("chapter-1").exceptionOrNull())
        assertTrue(mismatchFailure.message.orEmpty().contains("non-BOOK"))
    }

    @Test
    fun `unavailable resources fail with their availability before any resolver access`() = runTest {
        val resolver = FakeExternalResolver(emptyMap(), canResolveAppReferences = false)
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        id = "paid",
                        availability = BookResourceAvailability.PURCHASE_REQUIRED,
                        location = BookResourceLocation.RemoteRequest("https://example.invalid/paid"),
                    ),
                    resource(
                        id = "app",
                        location = BookResourceLocation.AppReference("download:42"),
                    ),
                ),
            ),
            resolver = resolver,
        )

        val paid = assertIs<BookResourceUnavailableException>(session.openResource("paid").exceptionOrNull())
        val app = assertIs<BookResourceUnavailableException>(session.openResource("app").exceptionOrNull())

        assertEquals("paid", paid.resourceId)
        assertEquals(BookResourceAvailability.PURCHASE_REQUIRED, paid.availability)
        assertEquals(BookResourceAvailability.UNSUPPORTED_APP_ACCESS, app.availability)
        assertEquals(
            BookResourceAvailability.UNSUPPORTED_APP_ACCESS,
            session.getResource("app").getOrThrow().availability,
        )
        assertTrue(resolver.requests.isEmpty())
    }
}
