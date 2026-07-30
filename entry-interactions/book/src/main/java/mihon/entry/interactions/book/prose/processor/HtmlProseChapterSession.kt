package mihon.entry.interactions.book.prose

import mihon.book.api.BookContentResource
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookPublication
import mihon.book.api.BookReadingDirection
import mihon.book.api.BookResource
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookPublicationResourceDependencies
import mihon.entry.interactions.book.BookPublicationSession
import mihon.entry.interactions.book.BookResourceRequirement
import mihon.entry.interactions.book.document.reader.BookDocumentResourceLoader
import mihon.entry.interactions.book.document.render.PreparedBookDocument

/** Built-in reader processor for one source-normalized prose chapter. */
internal class HtmlProseChapterSession(
    publicationId: String,
    revision: String,
    resource: BookContentResource,
    val document: PreparedBookDocument,
    content: BookContentSession,
) : BookPublicationSession, BookPublicationResourceDependencies {
    val resourceId: String = resource.id
    val resourceLoader: BookDocumentResourceLoader = HtmlProseResourceLoader(content)
    override val requiredResourceIds: Set<String> = document.document.resourceIds
    override val resourceRequirements: Map<String, BookResourceRequirement> =
        document.document.resourceRequirements().also { requirements ->
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
