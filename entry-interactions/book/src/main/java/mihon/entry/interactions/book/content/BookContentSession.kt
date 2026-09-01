package mihon.entry.interactions.book.content

import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookContentResourcePage
import java.io.File
import java.io.InputStream

internal interface BookContentSession : AutoCloseable {
    val descriptor: BookContentDescriptor
    val publicationId: String
    val revision: String
    val languages: List<String>
        get() = emptyList()
    val catalogRevision: String?
    val catalogCoverage: BookCatalogCoverage
    val resourceHierarchy: List<BookContentResourceGroup>
    val primaryResourceIds: List<String>

    suspend fun listResources(
        cursor: String? = null,
        limit: Int = DEFAULT_RESOURCE_PAGE_SIZE,
    ): Result<BookContentResourcePage>

    suspend fun getResource(resourceId: String): Result<BookContentResource>

    suspend fun openResource(resourceId: String, range: BookByteRange? = null): Result<OpenedBookResource>

    suspend fun materializeResource(resourceId: String): Result<MaterializedBookResource>

    companion object {
        const val DEFAULT_RESOURCE_PAGE_SIZE = 100
    }
}

internal data class BookByteRange(
    val startInclusive: Long,
    val endExclusive: Long? = null,
) {
    init {
        require(startInclusive >= 0) { "range start must not be negative" }
        require(endExclusive == null || endExclusive > startInclusive) {
            "range end must be greater than start"
        }
    }
}

internal interface OpenedBookResource : AutoCloseable {
    val metadata: BookContentResource
    val stream: InputStream
}

internal interface MaterializedBookResource : AutoCloseable {
    val metadata: BookContentResource
    val file: File

    /** Marks materialized bytes as invalid so a later acquisition cannot reuse them. */
    fun invalidate() = Unit
}
