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

internal fun totalBookProgression(
    chapters: List<EntryChapter>,
    chapterId: Long,
    chapterProgression: Float,
): Double {
    val index = chapters.indexOfFirst { it.id == chapterId }.coerceAtLeast(0)
    return ((index + chapterProgression) / chapters.size.coerceAtLeast(1)).coerceIn(0f, 1f).toDouble()
}
