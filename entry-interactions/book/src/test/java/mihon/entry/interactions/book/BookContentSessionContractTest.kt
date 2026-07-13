package mihon.entry.interactions.book

import kotlinx.coroutines.test.runTest
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookResourceCacheState
import mihon.book.api.BookResourceCapability
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BookContentSessionContractTest {

    @Test
    fun `resource catalog is cursor paged and keeps stable identities`() = runTest {
        val firstSession = FakeBookContentSession()
        val firstPage = firstSession.listResources(limit = 2).getOrThrow()
        val secondPage = firstSession.listResources(cursor = firstPage.nextCursor, limit = 2).getOrThrow()

        assertEquals(listOf("chapter-1", "chapter-2"), firstPage.resources.map { it.id })
        assertEquals("2", firstPage.nextCursor)
        assertEquals(listOf("chapter-3"), secondPage.resources.map { it.id })
        assertEquals(null, secondPage.nextCursor)

        val recreatedSession = FakeBookContentSession()
        assertEquals(
            firstSession.listResources(limit = 10).getOrThrow().resources.map { it.id },
            recreatedSession.listResources(limit = 10).getOrThrow().resources.map { it.id },
        )
    }

    @Test
    fun `ranged access returns a leased slice and closes independently`() = runTest {
        val session = FakeBookContentSession()
        val opened = session.openResource("chapter-2", BookByteRange(1, 4)).getOrThrow()
        val fakeOpened = opened as FakeOpenedResource

        assertContentEquals("hap".encodeToByteArray(), opened.stream.readBytes())
        assertFalse(fakeOpened.closed)

        opened.close()

        assertTrue(fakeOpened.closed)
        assertFalse(session.closed)
    }

    @Test
    fun `resource capabilities reject unsupported access`() = runTest {
        val session = FakeBookContentSession()

        assertFailsWith<IllegalStateException> {
            session.materializeResource("chapter-1").getOrThrow()
        }
        assertFailsWith<IllegalArgumentException> {
            BookByteRange(startInclusive = 4, endExclusive = 4)
        }
    }
}

private class FakeBookContentSession : BookContentSession {
    override val descriptor = BookContentDescriptor(format = "application/vnd.katari.book.serialized+json")
    override val publicationId = "book:fixture"
    override val revision = "v1"
    override val catalogRevision = "catalog-v1"
    override val catalogCoverage = BookCatalogCoverage.COMPLETE
    override val resourceHierarchy = emptyList<BookContentResourceGroup>()
    override val primaryResourceIds = listOf("chapter-1")
    var closed = false
        private set

    private val content = linkedMapOf(
        "chapter-1" to "First chapter".encodeToByteArray(),
        "chapter-2" to "Chapter two".encodeToByteArray(),
        "chapter-3" to "Third chapter".encodeToByteArray(),
    )
    private val resources = content.map { (id, bytes) ->
        BookContentResource(
            id = id,
            mediaType = "text/html",
            size = bytes.size.toLong(),
            revision = revision,
            cacheState = BookResourceCacheState.CACHED,
            capabilities = setOf(BookResourceCapability.STREAM, BookResourceCapability.RANGE),
        )
    }

    override suspend fun listResources(cursor: String?, limit: Int): Result<BookContentResourcePage> = runCatching {
        check(!closed) { "Session is closed" }
        require(limit > 0) { "Page limit must be positive" }
        val offset = cursor?.toIntOrNull() ?: 0
        require(offset in 0..resources.size) { "Invalid resource cursor" }
        val page = resources.drop(offset).take(limit)
        BookContentResourcePage(
            resources = page,
            nextCursor = (offset + page.size).takeIf { it < resources.size }?.toString(),
        )
    }

    override suspend fun getResource(resourceId: String): Result<BookContentResource> = runCatching {
        check(!closed) { "Session is closed" }
        resources.firstOrNull { it.id == resourceId } ?: throw NoSuchElementException(resourceId)
    }

    override suspend fun openResource(resourceId: String, range: BookByteRange?): Result<OpenedBookResource> =
        runCatching {
            val metadata = getResource(resourceId).getOrThrow()
            check(BookResourceCapability.STREAM in metadata.capabilities) { "Resource is not streamable" }
            val bytes = content.getValue(resourceId)
            val selected = if (range == null) {
                bytes
            } else {
                check(BookResourceCapability.RANGE in metadata.capabilities) { "Resource does not support ranges" }
                val endExclusive = (range.endExclusive ?: bytes.size.toLong()).coerceAtMost(bytes.size.toLong())
                require(range.startInclusive <= endExclusive) { "Range starts beyond the resource" }
                bytes.copyOfRange(range.startInclusive.toInt(), endExclusive.toInt())
            }
            FakeOpenedResource(metadata, ByteArrayInputStream(selected))
        }

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> = runCatching {
        val metadata = getResource(resourceId).getOrThrow()
        check(BookResourceCapability.MATERIALIZE in metadata.capabilities) { "Resource is not materializable" }
        object : MaterializedBookResource {
            override val metadata = metadata
            override val file: File
                get() = error("No fixture resource is materializable")

            override fun close() = Unit
        }
    }

    override fun close() {
        closed = true
    }
}

private class FakeOpenedResource(
    override val metadata: BookContentResource,
    override val stream: InputStream,
) : OpenedBookResource {
    var closed = false
        private set

    override fun close() {
        if (closed) return
        closed = true
        stream.close()
    }
}
