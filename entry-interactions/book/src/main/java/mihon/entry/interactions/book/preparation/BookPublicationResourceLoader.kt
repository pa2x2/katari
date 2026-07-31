package mihon.entry.interactions.book.preparation

internal data class BookPublicationResource(
    val resourceId: String,
    val mediaType: String?,
    val bytes: ByteArray,
)

/** Protected, publication-scoped resource access available to every prepared model renderer. */
internal interface BookPublicationResourceLoader {
    suspend fun load(
        resourceId: String,
        acceptedMediaTypes: Set<String>,
        maxBytes: Int,
    ): Result<BookPublicationResource>
}
