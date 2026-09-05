package mihon.entry.interactions.book.content

import mihon.book.api.BookContentResource

internal fun BookContentResource.fileSuffix(): String = when (mediaType.normalizedBookMediaType()) {
    "application/epub+zip" -> ".epub"
    "text/html", "application/xhtml+xml" -> ".html"
    "text/plain" -> ".txt"
    else -> ".bin"
}
