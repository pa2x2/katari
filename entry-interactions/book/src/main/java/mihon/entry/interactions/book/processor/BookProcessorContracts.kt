package mihon.entry.interactions.book

import android.content.Context
import android.content.Intent
import mihon.book.api.BookCatalogCoverage
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResource
import mihon.book.api.BookContentResourceGroup
import mihon.book.api.BookContentResourcePage
import mihon.book.api.BookFailure
import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import mihon.entry.viewer.settings.ReaderCapabilityId
import java.io.File
import java.io.InputStream

internal interface BookContentSession : AutoCloseable {
    val descriptor: BookContentDescriptor
    val publicationId: String
    val revision: String
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

internal interface BookProcessor {
    /** Stable across app updates so a remembered user choice can be restored. */
    val id: String

    /** User-facing processor name for the compatibility chooser. */
    val displayName: String

    /** App-level viewer settings surface configured by this processor, when one exists. */
    val viewerSettingsSurfaceId: String?
        get() = null

    /** Capabilities discoverable before a publication is opened. */
    val potentialReaderCapabilities: Set<ReaderCapabilityId>
        get() = emptySet()

    fun supports(descriptor: BookContentDescriptor): Boolean

    /** Creates the processor-owned reader UI entry point for a prepared BOOK session. */
    fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent

    suspend fun open(content: BookContentSession): BookOpenResult

    /** Capabilities which are effective for this concrete opened publication. */
    fun readerCapabilities(session: BookPublicationSession): Set<ReaderCapabilityId> = emptySet()
}

internal data class BookReaderRequest(
    val entryId: Long,
    val chapterId: Long,
)

internal sealed interface BookOpenResult {
    data class Success(val session: BookPublicationSession) : BookOpenResult
    data class Failure(val failure: BookFailure) : BookOpenResult
}

internal interface BookPublicationSession : AutoCloseable {
    val publication: BookPublication

    fun validate(locator: BookLocator): Boolean

    suspend fun reconcileMigratedLocator(locator: BookLocator): BookLocator? =
        locator.takeIf(::validate)
}

/** Publication-scoped resources that must accompany the primary resource offline. */
internal interface BookPublicationResourceDependencies {
    val requiredResourceIds: Set<String>
    val resourceRequirements: Map<String, BookResourceRequirement>
        get() = emptyMap()
}

internal data class BookResourceRequirement(
    val acceptedMediaTypes: Set<String>,
    val maxBytes: Int,
    val contentKind: BookResourceContentKind,
) {
    init {
        require(acceptedMediaTypes.isNotEmpty()) { "required BOOK resource media types must not be empty" }
        require(acceptedMediaTypes.none(String::isBlank)) {
            "required BOOK resource media types must not be blank"
        }
        require(maxBytes > 0) { "required BOOK resource byte limit must be positive" }
    }
}

internal enum class BookResourceContentKind {
    RASTER_IMAGE,
    FONT,
}
