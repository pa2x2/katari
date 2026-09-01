package mihon.entry.interactions.book.preparation

import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailure
import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import mihon.book.api.model.BookPublicationModel
import mihon.book.api.model.BookPublicationModelDescriptor
import mihon.entry.interactions.book.content.BookContentSession

/** Converts source-facing BOOK resources into one prepared publication model. */
internal interface BookContentPreparer {
    /** Stable identity used for diagnostics and deterministic registration. */
    val id: String

    /** Model family produced for every descriptor accepted by [supports]. */
    val outputModel: BookPublicationModelDescriptor

    fun supports(descriptor: BookContentDescriptor): Boolean

    suspend fun prepare(content: BookContentSession): BookPreparationResult
}

internal sealed interface BookPreparationResult {
    data class Success(val publication: PreparedBookPublication) : BookPreparationResult
    data class Failure(
        val failure: BookFailure,
        val canRetry: Boolean,
    ) : BookPreparationResult
}

/** Owns a prepared model and the runtime resources required to render it. */
internal interface PreparedBookPublication : AutoCloseable {
    val model: BookPublicationModel
    val publication: BookPublication
    val resourceLoader: BookPublicationResourceLoader

    /** Exact content identity used to decide whether a persisted locator can be restored. */
    val locatorRevision: String?
        get() = null

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
    DOCUMENT_IMAGE,
    FONT,
}
