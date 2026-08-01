package mihon.entry.interactions.book.document.preparation

import mihon.book.api.BookLocator
import mihon.book.api.BookPublication
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.entry.interactions.book.preparation.BookPublicationResourceDependencies
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import mihon.entry.interactions.book.preparation.BookResourceRequirement
import mihon.entry.interactions.book.preparation.PreparedBookPublication

/** Runtime owner for a canonical document model and its protected subordinate-resource access. */
internal class PreparedBookDocumentPublication(
    override val publication: BookPublication,
    override val model: BookDocumentPublicationModel,
    override val resourceLoader: BookPublicationResourceLoader,
) : PreparedBookPublication, BookPublicationResourceDependencies {
    init {
        require(publication.readingOrder.size == 1) {
            "A prepared single-document publication must have exactly one reading-order resource"
        }
        require(model.documents.singleOrNull()?.resourceId == publication.readingOrder.single().id) {
            "The prepared document model must match its publication reading order"
        }
    }

    val resourceId: String = publication.readingOrder.single().id
    val document get() = checkNotNull(model.document(resourceId))
    override val requiredResourceIds: Set<String> = document.resourceIds
    override val resourceRequirements: Map<String, BookResourceRequirement> =
        document.resourceRequirements().also { requirements ->
            require(requirements.keys == requiredResourceIds) {
                "Every required prose resource must declare offline validation constraints"
            }
        }

    override fun validate(locator: BookLocator): Boolean =
        locator.resourceId == resourceId &&
            locator.progression?.let { it.isFinite() && it in 0.0..1.0 } != false

    override suspend fun reconcileMigratedLocator(locator: BookLocator): BookLocator? {
        if (validate(locator)) return locator
        val progression = locator.progression ?: locator.totalProgression ?: return null
        return BookLocator(
            resourceId = resourceId,
            progression = progression,
            totalProgression = locator.totalProgression,
            textContext = locator.textContext,
        )
    }

    override fun close() = Unit
}
