package mihon.entry.interactions.book.document.reader

import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.render.PreparedBookDocument

internal data class BookDocumentSection<T>(
    val key: String,
    val owner: T,
    val document: PreparedBookDocument,
    val initialPosition: BookDocumentPosition,
    val resourceLoader: BookDocumentResourceLoader?,
) {
    init {
        require(key.isNotBlank()) { "document section key must not be blank" }
        require(document.document.contains(initialPosition)) { "initial position must belong to the section document" }
    }
}
