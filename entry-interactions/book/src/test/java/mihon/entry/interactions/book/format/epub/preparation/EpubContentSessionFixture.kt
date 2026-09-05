package mihon.entry.interactions.book.format.epub.preparation

import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCapability
import mihon.entry.interactions.book.content.BookByteRange
import mihon.entry.interactions.book.content.BookContentSession
import mihon.entry.interactions.book.content.MaterializedBookResource
import mihon.entry.interactions.book.content.OpenedBookResource
import mihon.entry.interactions.book.format.epub.EpubContract
import java.io.File

internal class EpubContentSessionFixture(
    private val file: File,
) : BookContentSession {
    private val resource = BookContentResource(
        id = "book.epub",
        mediaType = EpubContract.FORMAT,
        size = file.length(),
        revision = "resource-revision",
        availability = BookResourceAvailability.AVAILABLE,
        capabilities = setOf(BookResourceCapability.MATERIALIZE),
    )
    override val descriptor = BookContentDescriptor(EpubContract.FORMAT)
    override val publicationId = "publication"
    override val revision = "publication-revision"
    override val catalogRevision: String? = null
    override val catalogCoverage = BookCatalogCoverage.COMPLETE
    override val resourceHierarchy: List<BookContentResourceGroup> = emptyList()
    override val primaryResourceIds = listOf(resource.id)

    override suspend fun listResources(cursor: String?, limit: Int) =
        Result.success(BookContentResourcePage(listOf(resource)))

    override suspend fun getResource(resourceId: String) = Result.success(resource)

    override suspend fun openResource(resourceId: String, range: BookByteRange?): Result<OpenedBookResource> =
        error("The EPUB preparer must materialize its primary resource")

    override suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource> = Result.success(
        object : MaterializedBookResource {
            override val metadata = resource
            override val file = this@EpubContentSessionFixture.file
            override fun close() = Unit
        },
    )

    override fun close() = Unit
}
