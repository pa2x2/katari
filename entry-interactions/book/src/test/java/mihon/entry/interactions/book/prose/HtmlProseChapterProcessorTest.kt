package mihon.entry.interactions.book.prose

import kotlinx.coroutines.test.runTest
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookFailureReason
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCapability
import mihon.entry.interactions.book.BookByteRange
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookOpenResult
import mihon.entry.interactions.book.MaterializedBookResource
import mihon.entry.interactions.book.OpenedBookResource
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HtmlProseChapterProcessorTest {
    private val processor = HtmlProseChapterProcessor()

    @Test
    fun `supports only unprotected prose chapter html`() {
        assertTrue(processor.supports(BookContentDescriptor("text/html", profile = "prose-chapter")))
        assertFalse(processor.supports(BookContentDescriptor("text/html")))
        assertFalse(processor.supports(BookContentDescriptor("text/html", profile = "web-novel")))
        assertFalse(
            processor.supports(
                BookContentDescriptor("text/html", profile = "prose-chapter", protection = "vendor-drm"),
            ),
        )
    }

    @Test
    fun `opens one selected chapter and sanitizes active or external content`() = runTest {
        val content = TestProseContentSession(
            html = """
                <h1>Chapter 7</h1>
                <p onclick="steal()">Hello <em>reader</em>.</p>
                <script>steal()</script>
                <a href="https://example.com">external</a>
                <a href="#note">note</a>
                <img src="https://example.com/tracker.png">
            """.trimIndent(),
        )

        val result = assertIs<BookOpenResult.Success>(processor.open(content))
        val session = assertIs<HtmlProseChapterSession>(result.session)

        assertEquals(listOf("chapter-7"), session.publication.readingOrder.map { it.id })
        assertTrue(session.bodyHtml.contains("<em>reader</em>"))
        assertTrue(session.bodyHtml.contains("href=\"#note\""))
        assertFalse(session.bodyHtml.contains("onclick"))
        assertFalse(session.bodyHtml.contains("<script"))
        assertFalse(session.bodyHtml.contains("https://example.com"))
        assertFalse(session.bodyHtml.contains("<img"))
        assertEquals(1, content.openCount)
    }

    @Test
    fun `rejects a sibling catalog instead of constructing a multi chapter publication`() = runTest {
        val content = TestProseContentSession(
            html = "<p>Selected chapter</p>",
            primaryResourceIds = listOf("chapter-7", "chapter-8"),
        )

        val result = assertIs<BookOpenResult.Failure>(processor.open(content))

        assertEquals(BookFailureReason.CONTENT_UNAVAILABLE, result.failure.reason)
        assertEquals(0, content.openCount)
    }

    @Test
    fun `does not open purchase required chapter content`() = runTest {
        val content = TestProseContentSession(
            html = "<p>Preview must not be rendered as the chapter.</p>",
            availability = BookResourceAvailability.PURCHASE_REQUIRED,
        )

        val result = assertIs<BookOpenResult.Failure>(processor.open(content))

        assertEquals(BookFailureReason.CONTENT_UNAVAILABLE, result.failure.reason)
        assertEquals(0, content.openCount)
    }
}

private class TestProseContentSession(
    private val html: String,
    availability: BookResourceAvailability = BookResourceAvailability.AVAILABLE,
    override val primaryResourceIds: List<String> = listOf("chapter-7"),
) : BookContentSession {
    override val descriptor = BookContentDescriptor("text/html", profile = "prose-chapter")
    override val publicationId = "source:novel"
    override val revision = "unversioned"
    override val catalogRevision: String? = null
    override val catalogCoverage = BookCatalogCoverage.PARTIAL
    override val resourceHierarchy = emptyList<BookContentResourceGroup>()
    private val resource = BookContentResource(
        id = "chapter-7",
        title = "Chapter 7",
        mediaType = "text/html",
        size = html.encodeToByteArray().size.toLong(),
        availability = availability,
        capabilities = setOf(BookResourceCapability.STREAM),
    )
    var openCount = 0
        private set

    override suspend fun listResources(cursor: String?, limit: Int): Result<BookContentResourcePage> =
        Result.success(BookContentResourcePage(listOf(resource)))

    override suspend fun getResource(resourceId: String): Result<BookContentResource> =
        if (resourceId == resource.id) Result.success(resource) else Result.failure(NoSuchElementException(resourceId))

    override suspend fun openResource(resourceId: String, range: BookByteRange?): Result<OpenedBookResource> {
        if (resourceId != resource.id) return Result.failure(NoSuchElementException(resourceId))
        openCount++
        return Result.success(
            object : OpenedBookResource {
                override val metadata = resource
                override val stream = ByteArrayInputStream(html.encodeToByteArray())
                override fun close() = stream.close()
            },
        )
    }

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> =
        Result.failure(UnsupportedOperationException("Prose chapters are streamed"))

    override fun close() = Unit
}
