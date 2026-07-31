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
    fun `inline and external locations return bounded streams without exposing resolver details`() = runTest {
        val resolver = FakeExternalResolver(
            mapOf(
                "remote:https://example.invalid/book" to "remote-content".encodeToByteArray(),
                "local:content://app.katari/book/1" to "local-content".encodeToByteArray(),
                "app:download:42" to "app-content".encodeToByteArray(),
            ),
        )
        val remote = BookResourceLocation.RemoteRequest(
            "https://example.invalid/book",
            headers = mapOf("Authorization" to "secret"),
        )
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource("inline", location = inline("inline-content")),
                    resource("remote", location = remote),
                    resource("local", location = BookResourceLocation.LocalUri("content://app.katari/book/1")),
                    resource("app", location = BookResourceLocation.AppReference("download:42")),
                ),
            ),
            resolver = resolver,
        )

        session.openResource("inline", BookByteRange(1, 4)).getOrThrow().use { opened ->
            assertEquals("nli", opened.stream.bufferedReader().readText())
        }
        session.openResource("remote", BookByteRange(7, 14)).getOrThrow().use { opened ->
            assertEquals("content", opened.stream.bufferedReader().readText())
        }
        session.openResource("local").getOrThrow().use { opened ->
            assertEquals("local-content", opened.stream.bufferedReader().readText())
        }
        session.openResource("app").getOrThrow().use { opened ->
            assertEquals("app-content", opened.stream.bufferedReader().readText())
        }

        assertEquals(remote, resolver.requests.first().first)
        assertEquals(BookByteRange(7, 14), resolver.requests.first().second)
        assertEquals(3, resolver.closeCount.get())
    }

    @Test
    fun `inline range larger than addressable content fails as a normal resource error`() = runTest {
        val session = session(
            media = bookMedia(
                resources = listOf(resource("inline", location = inline("content"))),
            ),
        )

        assertTrue(session.openResource("inline", BookByteRange(Long.MAX_VALUE)).isFailure)
    }

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
    fun `availability failure remains structured and does not open a resolver`() = runTest {
        val resolver = FakeExternalResolver(emptyMap())
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        id = "paid",
                        availability = BookResourceAvailability.PURCHASE_REQUIRED,
                        location = BookResourceLocation.RemoteRequest("https://example.invalid/paid"),
                    ),
                ),
            ),
            resolver = resolver,
        )

        val failure = assertIs<BookResourceUnavailableException>(
            session.openResource("paid").exceptionOrNull(),
        )

        assertEquals("paid", failure.resourceId)
        assertEquals(BookResourceAvailability.PURCHASE_REQUIRED, failure.availability)
        assertTrue(resolver.requests.isEmpty())
    }

    @Test
    fun `app references without an app resolver report unsupported access`() = runTest {
        val resolver = FakeExternalResolver(emptyMap(), canResolveAppReferences = false)
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        id = "app",
                        location = BookResourceLocation.AppReference("download:42"),
                    ),
                ),
            ),
            resolver = resolver,
        )

        val metadata = session.getResource("app").getOrThrow()
        val failure = assertIs<BookResourceUnavailableException>(
            session.openResource("app").exceptionOrNull(),
        )

        assertEquals(BookResourceAvailability.UNSUPPORTED_APP_ACCESS, metadata.availability)
        assertTrue(metadata.capabilities.isEmpty())
        assertEquals(BookResourceAvailability.UNSUPPORTED_APP_ACCESS, failure.availability)
        assertTrue(resolver.requests.isEmpty())
    }
}
