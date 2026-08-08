package mihon.entry.interactions.book.document.reader

import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.document.render.toPreparedBookDocument
import mihon.entry.interactions.book.reader.OpenedBookReaderSession
import tachiyomi.domain.entry.model.EntryChapter

/** Resolves one opened generic BOOK session into the semantic document viewer boundary. */
internal fun OpenedBookReaderSession.toDocumentSection(
    retainedLocator: mihon.book.api.BookLocator?,
): BookDocumentSection<EntryChapter>? {
    val publication = preparedPublication as? PreparedBookDocumentPublication ?: return null
    val document = publication.document.toPreparedBookDocument()
    val initialPosition = retainedLocator
        ?.let(document.document::resolvePosition)
        ?: initialLocator?.let(document.document::resolvePosition)
        ?: document.document.positionAtProgression(0f)
    return BookDocumentSection(
        key = chapter.id.toString(),
        owner = chapter,
        document = document,
        initialPosition = initialPosition,
        resourceLoader = publication.resourceLoader,
    )
}

internal fun <T> BookDocumentSection<T>.fromBeginningForExplicitNavigation(): BookDocumentSection<T> = copy(
    initialPosition = document.document.positionAtProgression(0f),
)
