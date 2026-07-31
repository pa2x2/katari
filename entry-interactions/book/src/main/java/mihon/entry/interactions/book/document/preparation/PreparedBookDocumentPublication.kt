package mihon.entry.interactions.book.document.preparation

import mihon.book.api.BookContentResource
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookPublication
import mihon.book.api.BookReadingDirection
import mihon.book.api.BookResource
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.entry.interactions.book.BookPublicationResourceDependencies
import mihon.entry.interactions.book.BookPublicationResourceLoader
import mihon.entry.interactions.book.BookResourceRequirement
import mihon.entry.interactions.book.PreparedBookPublication

/** Runtime owner for a canonical document model and its protected subordinate-resource access. */
internal class PreparedBookDocumentPublication(
    publicationId: String,
    revision: String,
    resource: BookContentResource,
    override val model: BookDocumentPublicationModel,
    override val resourceLoader: BookPublicationResourceLoader,
) : PreparedBookPublication, BookPublicationResourceDependencies {
    val resourceId: String = resource.id
    val document get() = checkNotNull(model.document(resourceId))
    override val requiredResourceIds: Set<String> = document.resourceIds
    override val resourceRequirements: Map<String, BookResourceRequirement> =
        document.resourceRequirements().also { requirements ->
            require(requirements.keys == requiredResourceIds) {
                "Every required prose resource must declare offline validation constraints"
            }
        }

    override val publication = BookPublication(
        id = publicationId,
        revision = revision,
        title = resource.title,
        languages = emptyList(),
        readingDirection = BookReadingDirection.LEFT_TO_RIGHT,
        readingOrder = listOf(
            BookResource(
                id = resource.id,
                mediaType = resource.mediaType,
                title = resource.title,
            ),
        ),
        navigation = listOf(
            BookNavigationItem(
                title = resource.title,
                target = BookLocator(resourceId = resource.id, progression = 0.0),
            ),
        ),
    )

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
