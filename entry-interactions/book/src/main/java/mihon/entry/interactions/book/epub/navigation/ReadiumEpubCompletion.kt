package mihon.entry.interactions.book.epub

import mihon.book.api.BookLocator
import mihon.book.api.BookResource

internal fun isEpubPublicationComplete(locator: BookLocator, readingOrder: List<BookResource>): Boolean {
    locator.totalProgression?.let { return it >= EPUB_COMPLETION_THRESHOLD }
    val lastResource = readingOrder.lastOrNull() ?: return false
    return locator.resourceId == lastResource.id &&
        locator.progression?.let { it >= EPUB_COMPLETION_THRESHOLD } == true
}

private const val EPUB_COMPLETION_THRESHOLD = 0.995
