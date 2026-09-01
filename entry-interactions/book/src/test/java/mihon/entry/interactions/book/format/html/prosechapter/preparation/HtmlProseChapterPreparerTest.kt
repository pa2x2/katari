package mihon.entry.interactions.book.format.html.prosechapter.preparation

import kotlinx.coroutines.test.runTest
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookFailureReason
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCapability
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.entry.interactions.book.content.BookByteRange
import mihon.entry.interactions.book.content.BookContentSession
import mihon.entry.interactions.book.content.MaterializedBookResource
import mihon.entry.interactions.book.content.OpenedBookResource
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.preparation.BookPreparationResult
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HtmlProseChapterPreparerTest {
    private val preparer = HtmlProseChapterPreparer()

    @Test
    fun `only the exact unprotected prose chapter descriptor is supported`() {
        assertTrue(preparer.supports(HtmlProseChapterContract.descriptor))
        assertFalse(preparer.supports(BookContentDescriptor("text/html", profile = "prose")))
        assertFalse(
            preparer.supports(
                BookContentDescriptor("text/html", profile = "prose-chapter", protection = "drm"),
            ),
        )
    }

    @Test
    fun `preparation produces a generic document publication`() = runTest {
        val result = assertIs<BookPreparationResult.Success>(
            preparer.prepare(
                FakeHtmlContentSession(
                    "<html lang='fr-FR'><body><h1 id='start'>Title</h1><p>Body</p></body></html>",
                    languages = listOf("en"),
                ),
            ),
        )

        val model = assertIs<BookDocumentPublicationModel>(result.publication.model)
        assertEquals("book.document", model.descriptor.id)
        assertEquals(listOf("chapter.html"), result.publication.publication.readingOrder.map { it.id })
        assertEquals(listOf("fr-FR", "en"), result.publication.publication.languages)
        assertEquals(setOf("start"), model.documents.single().anchors.keys)
    }

    @Test
    fun `resource media mismatch fails before HTML is opened`() = runTest {
        val content = FakeHtmlContentSession("<p>Body</p>", mediaType = "application/xhtml+xml")
        val result = assertIs<BookPreparationResult.Failure>(preparer.prepare(content))

        assertEquals(BookFailureReason.FORMAT_UNSUPPORTED, result.failure.reason)
        assertFalse(content.opened)
    }
}

private class FakeHtmlContentSession(
    html: String,
    mediaType: String? = "text/html; charset=utf-8",
    override val languages: List<String> = emptyList(),
) : BookContentSession {
    private val bytes = html.encodeToByteArray()
    private val resource = BookContentResource(
        id = "chapter.html",
        title = "Chapter",
        mediaType = mediaType,
        size = bytes.size.toLong(),
        revision = "resource-revision",
        availability = BookResourceAvailability.AVAILABLE,
        capabilities = setOf(BookResourceCapability.STREAM),
    )
    var opened = false
        private set

    override val descriptor = HtmlProseChapterContract.descriptor
    override val publicationId = "publication"
    override val revision = "publication-revision"
    override val catalogRevision: String? = null
    override val catalogCoverage = BookCatalogCoverage.COMPLETE
    override val resourceHierarchy: List<BookContentResourceGroup> = emptyList()
    override val primaryResourceIds = listOf(resource.id)

    override suspend fun listResources(cursor: String?, limit: Int) =
        Result.success(BookContentResourcePage(listOf(resource)))

    override suspend fun getResource(resourceId: String) = Result.success(resource)

    override suspend fun openResource(resourceId: String, range: BookByteRange?): Result<OpenedBookResource> {
        opened = true
        return Result.success(
            object : OpenedBookResource {
                override val metadata = resource
                override val stream = ByteArrayInputStream(bytes)
                override fun close() = stream.close()
            },
        )
    }

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> =
        error("The prose preparer does not materialize its primary resource")

    override fun close() = Unit
}
