package mihon.entry.interactions.book.document.reader

internal data class BookDocumentBinaryResource(
    val resourceId: String,
    val mediaType: String?,
    val bytes: ByteArray,
)

/**
 * Reader-facing access to publication-scoped subordinate resources.
 *
 * Implementations stay behind the BOOK content session so renderers never
 * receive provider requests, headers, credentials, or arbitrary URLs.
 */
internal interface BookDocumentResourceLoader {
    suspend fun load(
        resourceId: String,
        acceptedMediaTypes: Set<String>,
        maxBytes: Int,
    ): Result<BookDocumentBinaryResource>
}
