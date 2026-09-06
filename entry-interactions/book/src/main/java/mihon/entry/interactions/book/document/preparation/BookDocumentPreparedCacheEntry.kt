package mihon.entry.interactions.book.document.preparation

import kotlinx.serialization.Serializable

@Serializable
internal data class BookDocumentPreparedCacheEntry(
    val schemaVersion: Int,
    val key: BookDocumentPreparedCacheKey,
    val value: BookDocumentPreparedCacheValue,
)
