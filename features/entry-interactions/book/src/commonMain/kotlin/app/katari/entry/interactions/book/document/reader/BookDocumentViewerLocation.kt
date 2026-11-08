package app.katari.entry.interactions.book.document.reader

import app.katari.book.api.document.BookDocumentPosition

/** A semantic reading observation; exact restoration carries the request it completed. */
internal data class BookDocumentViewerLocation<T>(
    val section: BookDocumentSection<T>,
    val position: BookDocumentPosition,
    val progression: Float,
    val visualProgression: Float = progression,
    val restoredNavigationId: Long? = null,
    val viewportEndProgression: Float? = null,
)
