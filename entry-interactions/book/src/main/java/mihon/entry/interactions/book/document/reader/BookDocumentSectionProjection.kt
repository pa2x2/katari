package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.document.render.toPreparedBookDocument
import mihon.entry.interactions.book.reader.OpenedBookReaderSession
import tachiyomi.domain.entry.model.EntryChapter

/** Resolves one opened generic BOOK session into the semantic document viewer boundary. */
internal fun OpenedBookReaderSession.toDocumentSections(
    retainedLocator: mihon.book.api.BookLocator?,
): BookDocumentPublicationSections<EntryChapter>? {
    val publication = preparedPublication as? PreparedBookDocumentPublication ?: return null
    val candidateLocator = retainedLocator?.takeIf(publication::validate)
        ?: initialLocator?.takeIf(publication::validate)
    val progress = publication.progress
    val sections = publication.documents.map { source ->
        val document = source.toPreparedBookDocument()
        val initialPosition = candidateLocator
            ?.takeIf { it.resourceId == source.resourceId }
            ?.let(document.document::resolvePosition)
            ?: document.document.positionAtProgression(0f)
        BookDocumentSection(
            key = "${chapter.id}:${source.resourceId}",
            owner = chapter,
            document = document,
            initialPosition = initialPosition,
            resourceLoader = publication.resourceLoader,
            publicationProgress = progress,
        )
    }
    return BookDocumentPublicationSections(
        sections = sections,
        initialSectionKey = sections.firstOrNull { it.document.document.resourceId == candidateLocator?.resourceId }
            ?.key
            ?: sections.first().key,
    )
}

internal fun <T> BookDocumentPublicationSections<T>.fromBeginningForExplicitNavigation():
    BookDocumentPublicationSections<T> {
    val first = sections.first()
    return copy(
        sections = listOf(first.copy(initialPosition = first.document.document.positionAtProgression(0f))) +
            sections.drop(1),
        initialSectionKey = first.key,
    )
}

internal fun <T> BookDocumentSection<T>.fromBeginningForExplicitNavigation(): BookDocumentSection<T> = copy(
    initialPosition = document.document.positionAtProgression(0f),
)
