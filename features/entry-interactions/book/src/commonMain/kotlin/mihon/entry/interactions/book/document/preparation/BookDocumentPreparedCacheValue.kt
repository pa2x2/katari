package mihon.entry.interactions.book.document.preparation

import kotlinx.serialization.Serializable
import mihon.book.api.document.BookDocumentPublicationModel

@Serializable
internal data class BookDocumentPreparedCacheValue(
    val model: BookDocumentPublicationModel,
    val documentTitles: Map<String, String?>,
    val derivedResources: List<BookDocumentCachedResource> = emptyList(),
    val remoteResources: List<BookDocumentCachedRemoteResource> = emptyList(),
)

@Serializable
internal data class BookDocumentCachedResource(
    val resourceId: String,
    val mediaType: String,
    val bytes: ByteArray,
)

@Serializable
internal data class BookDocumentCachedRemoteResource(
    val resourceId: String,
    val url: String,
    val type: String,
)
