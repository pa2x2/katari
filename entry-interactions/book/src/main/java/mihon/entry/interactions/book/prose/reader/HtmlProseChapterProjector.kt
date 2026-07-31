package mihon.entry.interactions.book.prose

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.book.api.BookLocator
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.PreparedBookPublication
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.render.toPreparedBookDocument
import tachiyomi.domain.entry.model.EntryChapter

internal data class HtmlProseChapterProjection(
    val chapter: HtmlProseLoadedChapter,
    val locator: BookLocator,
)

/** Owns the Android rendering projection of a canonical structured-document model. */
internal class HtmlProseChapterProjector(
    private val projectionDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun project(
        owner: EntryChapter,
        publication: PreparedBookPublication,
        locator: BookLocator?,
        reusableDocument: PreparedBookDocument? = null,
    ): HtmlProseChapterProjection? {
        val model = publication.model as? BookDocumentPublicationModel ?: return null
        val validatedLocator = locator
            ?.takeIf(publication::validate)
            ?.takeIf { model.document(it.resourceId) != null }
        val resourceId = validatedLocator?.resourceId
            ?: publication.publication.readingOrder
                .firstNotNullOfOrNull { resource -> model.document(resource.id)?.resourceId }
            ?: return null
        val document = model.document(resourceId) ?: return null
        val effectiveLocator = validatedLocator ?: BookLocator(resourceId, progression = 0.0)
        val preparedDocument = reusableDocument
            ?.takeIf { it.document == document }
            ?: withContext(projectionDispatcher) { document.toPreparedBookDocument() }
        val initialPosition = document.resolvePosition(effectiveLocator)
            ?: document.positionAtProgression((effectiveLocator.progression ?: 0.0).toFloat())

        return HtmlProseChapterProjection(
            chapter = HtmlProseLoadedChapter(
                key = owner.id.toString(),
                owner = owner,
                document = preparedDocument,
                initialPosition = initialPosition,
                resourceLoader = publication.resourceLoader,
            ),
            locator = effectiveLocator,
        )
    }
}
