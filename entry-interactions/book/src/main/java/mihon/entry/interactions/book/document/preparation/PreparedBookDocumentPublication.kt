package mihon.entry.interactions.book.document.preparation

import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.book.api.document.BookDocumentPublicationProgress
import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.preparation.BookPublicationResourceDependencies
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.entry.interactions.book.preparation.BookRemoteResourceAuthorization
import mihon.entry.interactions.book.preparation.BookRemoteResourceRequest
import mihon.entry.interactions.book.preparation.BookResourceRequirement
import mihon.entry.interactions.book.preparation.PreparedBookPublication

/** Runtime owner for a canonical document model and its protected subordinate-resource access. */
internal class PreparedBookDocumentPublication(
    override val publication: BookPublication,
    override val model: BookDocumentPublicationModel,
    override val resourceLoader: BookPublicationResourceLoader,
    override val locatorRevision: String? = null,
    override val requiredResourceIds: Set<String> = model.documents.flatMapTo(linkedSetOf()) { it.resourceIds },
    override val resourceRequirements: Map<String, BookResourceRequirement> = model.documents
        .flatMap { it.resourceRequirements().entries }
        .associate { it.toPair() },
    private val closeAction: () -> Unit = {},
) : PreparedBookPublication, BookPublicationResourceDependencies, BookRemoteResourceAuthorization {
    init {
        require(publication.readingOrder.isNotEmpty()) {
            "A prepared document publication must have a non-empty reading order"
        }
        require(publication.readingOrder.all { resource -> model.document(resource.id) != null }) {
            "Every reading-order resource must have a prepared document"
        }
        require(resourceRequirements.keys == requiredResourceIds) {
            "Every required document resource must declare offline validation constraints"
        }
    }

    val documents get() = publication.readingOrder.map { resource -> checkNotNull(model.document(resource.id)) }

    val allDocuments get() = model.documents

    val progress by lazy { BookDocumentPublicationProgress(documents) }

    override fun progression(locator: BookLocator): Double? {
        val document = documents.firstOrNull { it.resourceId == locator.resourceId } ?: return null
        val local = locator.progression
            ?: document.resolvePosition(locator)?.let(document::progressionAt)?.toDouble()
            ?: return null
        return progress.totalProgression(document.resourceId, local)
    }

    override val remoteResourceRequests: Set<BookRemoteResourceRequest>
        get() = (resourceLoader as? BookRemoteResourceAuthorization)?.remoteResourceRequests.orEmpty()

    override fun authorizeRemoteOrigins(origins: Set<String>) {
        (resourceLoader as? BookRemoteResourceAuthorization)?.authorizeRemoteOrigins(origins)
            ?: require(origins.isEmpty()) { "This publication has no remote resource gateway" }
    }

    fun document(resourceId: String) = model.document(resourceId)

    override fun validate(locator: BookLocator): Boolean =
        model.document(locator.resourceId) != null &&
            locator.progression?.let { it.isFinite() && it in 0.0..1.0 } != false

    override suspend fun reconcileMigratedLocator(locator: BookLocator): BookLocator? = null

    override fun close() = closeAction()
}
