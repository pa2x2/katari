package mihon.entry.interactions.book.document.reader.navigation

import mihon.book.api.BookLocator
import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentPosition
import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication

internal data class BookDocumentNavigationDestination(
    val document: BookDocument,
    val position: BookDocumentPosition,
    val contextual: Boolean,
)

/** Explicit fragments must exist; only a document-start link may fall back to the beginning. */
internal fun BookDocument.navigationPosition(locator: BookLocator): BookDocumentPosition? {
    if (locator.resourceId != resourceId) return null
    if (locator.fragments.isNotEmpty()) {
        return locator.fragments.firstNotNullOfOrNull { fragment ->
            anchors[fragment] ?: blocks.firstOrNull { fragment in it.sourceFragments }
                ?.let { BookDocumentPosition(it.id, 0) }
        }
    }
    return resolvePosition(locator) ?: positionAtProgression(0f)
}

internal fun PreparedBookDocumentPublication.resolveNavigationDestination(
    locator: BookLocator,
    contextual: Boolean,
): BookDocumentNavigationDestination? {
    val document = document(locator.resourceId) ?: return null
    val position = document.navigationPosition(locator) ?: return null
    return BookDocumentNavigationDestination(
        document,
        position,
        contextual || publication.readingOrder.none { it.id == document.resourceId },
    )
}
