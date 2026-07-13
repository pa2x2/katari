package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.BookResourceCatalog
import eu.kanade.tachiyomi.source.entry.BookResourceHierarchyNode
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import eu.kanade.tachiyomi.source.entry.BookSourceResource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCacheState
import mihon.book.api.BookResourceCapability
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SourceBookContentSessionTest {

    @Test
    fun `catalog paging preserves source ordering identity and publication revision`() = runTest {
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource("third", order = 2, location = inline("third")),
                    resource(
                        "first",
                        title = "First chapter",
                        order = 0,
                        groupId = "volume-1",
                        location = inline("first"),
                    ),
                    resource("second", order = 1, location = inline("second")),
                ),
                initialResourceId = "first",
                initialLocation = inline("first"),
                hierarchy = listOf(
                    BookResourceHierarchyNode(
                        id = "volume-1",
                        title = "Volume 1",
                        resourceIds = listOf("first", "second"),
                    ),
                ),
            ),
        )

        val firstPage = session.listResources(limit = 2).getOrThrow()
        val secondPage = session.listResources(firstPage.nextCursor, limit = 2).getOrThrow()

        assertEquals("source:42:entry:/books/fixture", session.publicationId)
        assertEquals("publication-v2", session.revision)
        assertEquals("catalog-v3", session.catalogRevision)
        assertEquals(BookCatalogCoverage.COMPLETE, session.catalogCoverage)
        assertEquals("volume-1", session.resourceHierarchy.single().id)
        assertEquals(listOf("first", "second"), session.resourceHierarchy.single().resourceIds)
        assertEquals(listOf("first"), session.primaryResourceIds)
        assertEquals(listOf("first", "second"), firstPage.resources.map { it.id })
        assertEquals(listOf("third"), secondPage.resources.map { it.id })
        assertEquals(null, secondPage.nextCursor)
        assertEquals(BookResourceCacheState.CACHED, firstPage.resources.first().cacheState)
        assertEquals("First chapter", firstPage.resources.first().title)
        assertEquals(0L, firstPage.resources.first().order)
        assertEquals("volume-1", firstPage.resources.first().groupId)
        assertEquals(
            setOf(
                BookResourceCapability.STREAM,
                BookResourceCapability.RANGE,
                BookResourceCapability.MATERIALIZE,
            ),
            firstPage.resources.first().capabilities,
        )
    }

    @Test
    fun `publication discriminator extends rather than replaces default identity`() {
        val session = session(media = bookMedia(publicationKeyOverride = "epub"))

        assertEquals("source:42:entry:/books/fixture:publication:epub", session.publicationId)
    }

    @Test
    fun `catalog preserves source list order when explicit ordering is absent`() = runTest {
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource("zeta", location = inline("zeta")),
                    resource("alpha", location = inline("alpha")),
                ),
            ),
        )

        assertEquals(
            listOf("zeta", "alpha"),
            session.listResources(limit = 10).getOrThrow().resources.map { it.id },
        )
        assertTrue(session.listResources(cursor = "invalid", limit = 10).isFailure)
        assertTrue(session.listResources(limit = 501).isFailure)
    }

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
    fun `materialized files are bounded and owned by their leases`() = runTest {
        val directory = Files.createTempDirectory("katari-book-session-test").toFile()
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        "epub",
                        mediaType = "application/epub+zip",
                        location = BookResourceLocation.InlineBytes("epub-content".encodeToByteArray()),
                    ),
                ),
                initialResourceId = "epub",
                initialLocation = BookResourceLocation.InlineBytes("epub-content".encodeToByteArray()),
            ),
            directory = directory,
        )

        val materialized = session.materializeResource("epub").getOrThrow()
        assertTrue(materialized.file.exists())
        assertTrue(materialized.file.name.endsWith(".epub"))
        assertEquals("epub-content", materialized.file.readText())

        materialized.close()
        assertFalse(materialized.file.exists())

        val outstanding = session.materializeResource("epub").getOrThrow()
        session.close()
        assertFalse(outstanding.file.exists())
        assertTrue(session.getResource("epub").isFailure)
    }

    @Test
    fun `declared oversized resource fails before external access`() = runTest {
        val resolver = FakeExternalResolver(emptyMap())
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        id = "huge",
                        size = 512L * 1024L * 1024L + 1L,
                        location = BookResourceLocation.RemoteRequest("https://example.invalid/huge"),
                    ),
                ),
            ),
            resolver = resolver,
        )

        assertTrue(session.materializeResource("huge").isFailure)
        assertTrue(resolver.requests.isEmpty())
    }

    @Test
    fun `session close releases outstanding streams once`() = runTest {
        val resolver = FakeExternalResolver(
            mapOf("remote:https://example.invalid/book" to "content".encodeToByteArray()),
        )
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        "remote",
                        location = BookResourceLocation.RemoteRequest("https://example.invalid/book"),
                    ),
                ),
            ),
            resolver = resolver,
        )

        session.openResource("remote").getOrThrow()
        session.close()
        session.close()

        assertEquals(1, resolver.closeCount.get())
    }

    @Test
    fun `cancellation propagates across the session result boundary`() = runTest {
        val session = session(
            media = bookMedia(
                resources = listOf(
                    resource(
                        "remote",
                        location = BookResourceLocation.RemoteRequest("https://example.invalid/book"),
                    ),
                ),
            ),
            resolver = object : BookExternalResourceResolver {
                override suspend fun open(
                    location: BookResourceLocation,
                    range: BookByteRange?,
                ): ExternalBookResource = throw CancellationException("cancelled")
            },
        )

        assertFailsWith<CancellationException> { session.openResource("remote") }
    }

    private fun session(
        media: EntryMedia.Book,
        source: UnifiedSource = source(),
        resolver: BookExternalResourceResolver = FakeExternalResolver(emptyMap()),
        directory: File = Files.createTempDirectory("katari-book-materialized").toFile(),
    ): SourceBookContentSession {
        return SourceBookContentSession(
            source = source,
            entry = Entry.create().copy(
                id = 1L,
                source = 42L,
                url = "/books/fixture",
                type = EntryType.BOOK,
            ),
            media = media,
            externalResolver = resolver,
            materializationDirectory = directory,
        )
    }

    private fun source(): UnifiedSource = mockk {
        every { id } returns 42L
        every { name } returns "Fixture"
    }

    private fun bookMedia(
        resources: List<BookSourceResource> = emptyList(),
        initialResourceId: String? = null,
        initialLocation: BookResourceLocation? = null,
        publicationKeyOverride: String? = null,
        hierarchy: List<BookResourceHierarchyNode> = emptyList(),
    ): EntryMedia.Book {
        return EntryMedia.Book(
            descriptor = BookContentDescriptor("application/vnd.katari.book+json"),
            publicationKeyOverride = publicationKeyOverride,
            publicationRevision = "publication-v2",
            catalog = BookResourceCatalog(
                resources = resources,
                revision = "catalog-v3",
                coverage = BookCatalogCoverage.COMPLETE,
            ),
            hierarchy = hierarchy,
            initialResourceId = initialResourceId,
            initialResourceLocation = initialLocation,
        )
    }

    private fun resource(
        id: String,
        title: String? = null,
        order: Long? = null,
        groupId: String? = null,
        mediaType: String? = null,
        size: Long? = null,
        availability: BookResourceAvailability = BookResourceAvailability.AVAILABLE,
        location: BookResourceLocation,
    ): BookSourceResource {
        return BookSourceResource(
            id = id,
            title = title,
            order = order,
            groupId = groupId,
            mediaType = mediaType,
            size = size,
            availability = availability,
            location = location,
        )
    }

    private fun inline(text: String): BookResourceLocation =
        BookResourceLocation.InlineText(text, "text/plain")
}

private class FakeExternalResolver(
    private val content: Map<String, ByteArray>,
) : BookExternalResourceResolver {
    val requests = mutableListOf<Pair<BookResourceLocation, BookByteRange?>>()
    val closeCount = AtomicInteger()

    override suspend fun open(
        location: BookResourceLocation,
        range: BookByteRange?,
    ): ExternalBookResource {
        requests += location to range
        val bytes = content.getValue(location.key())
        val start = range?.startInclusive?.toInt() ?: 0
        val end = range?.endExclusive?.coerceAtMost(bytes.size.toLong())?.toInt() ?: bytes.size
        val stream = ByteArrayInputStream(bytes, start, end - start)
        return object : ExternalBookResource {
            override val stream: InputStream = stream

            override fun close() {
                stream.close()
                closeCount.incrementAndGet()
            }
        }
    }
}

private fun BookResourceLocation.key(): String = when (this) {
    is BookResourceLocation.RemoteRequest -> "remote:$url"
    is BookResourceLocation.LocalUri -> "local:$uri"
    is BookResourceLocation.AppReference -> "app:$id"
    is BookResourceLocation.InlineBytes,
    is BookResourceLocation.InlineText,
    is BookResourceLocation.SourceChild,
    -> error("Location is not external: $this")
}
