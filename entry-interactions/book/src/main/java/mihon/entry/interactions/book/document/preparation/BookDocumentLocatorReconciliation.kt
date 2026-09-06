package mihon.entry.interactions.book.document.preparation

import mihon.book.api.BookLocator
import mihon.book.api.document.BookDocument
import mihon.book.api.document.locatorAt
import mihon.book.api.document.resolvePosition

/** Migrated block IDs belong to the source document; only content evidence can identify a new document. */
internal fun reconcileDocumentLocator(documents: List<BookDocument>, locator: BookLocator): BookLocator? {
    val contextualMatches = documents.mapNotNull { document ->
        val contextOnly = BookLocator(document.resourceId, textContext = locator.textContext)
        document.resolvePosition(contextOnly)?.let { document.locatorAt(it) }
    }
    if (contextualMatches.size == 1) return contextualMatches.single()
    if (contextualMatches.size > 1) return null

    val document = documents.singleOrNull { it.resourceId == locator.resourceId }
        ?: documents.singleOrNull()
        ?: return null
    // Source fragments may survive a publication update; generated block IDs must not win over text.
    val portable = locator.copy(
        resourceId = document.resourceId,
        fragments = locator.fragments.filter { it in document.anchors },
        extensions = emptyMap(),
    )
    return document.resolvePosition(portable)?.let(document::locatorAt)
}
