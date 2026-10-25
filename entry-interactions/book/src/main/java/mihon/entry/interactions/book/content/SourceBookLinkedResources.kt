package mihon.entry.interactions.book.content

import eu.kanade.tachiyomi.source.entry.BookLinkedResources
import eu.kanade.tachiyomi.source.entry.BookResourceLocation
import mihon.book.api.BookContentResource
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCacheState
import mihon.book.api.BookResourceCapability
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Resources an online session creates for images its documents name by URL or `data:` URI.
 *
 * They are registered while a document is prepared and live as long as the session, so the reader and the
 * downloader reach them by ID like catalog resources. Request headers stay here, out of processors' reach.
 */
internal class SourceBookLinkedResources(
    linkedResources: BookLinkedResources,
    primaryLocation: BookResourceLocation?,
) {
    private val baseUrl: HttpUrl? = (
        linkedResources.baseUrl
            ?: (primaryLocation as? BookResourceLocation.RemoteRequest)?.url
        )?.toHttpUrlOrNull()
    private val headers = linkedResources.headers
    private val lock = Any()
    private val records = LinkedHashMap<String, ResourceRecord>()
    private val inlineData = HashMap<String, ByteArray>()

    /** Returns the linked resource ID for [reference], or what to look up instead when it isn't linked. */
    fun resolve(reference: String): String? = when (val image = classifyBookImageReference(reference, baseUrl)) {
        BookImageReference.Unusable -> null
        is BookImageReference.Named -> image.resourceId
        is BookImageReference.Remote -> register(
            linkedImageResourceId(reference),
            BookResourceLocation.RemoteRequest(image.url.toString(), headers),
            data = null,
        )
        is BookImageReference.Data -> register(linkedImageResourceId(reference), location = null, data = image.bytes)
    }

    fun record(resourceId: String): ResourceRecord? = synchronized(lock) { records[resourceId] }

    fun data(resourceId: String): ByteArray? = synchronized(lock) { inlineData[resourceId] }

    // A hostile chapter can name any number of URLs; images past the limit read as their alternative text.
    private fun register(resourceId: String, location: BookResourceLocation?, data: ByteArray?): String? =
        synchronized(lock) {
            if (resourceId in records) return resourceId
            if (records.size >= MAX_LINKED_RESOURCES) return null
            records[resourceId] = ResourceRecord(
                id = resourceId,
                metadata = BookContentResource(
                    id = resourceId,
                    size = data?.size?.toLong(),
                    availability = BookResourceAvailability.AVAILABLE,
                    cacheState = if (data != null) BookResourceCacheState.CACHED else BookResourceCacheState.UNKNOWN,
                    capabilities = setOf(
                        BookResourceCapability.STREAM,
                        BookResourceCapability.RANGE,
                        BookResourceCapability.MATERIALIZE,
                    ),
                ),
                location = location,
            )
            data?.let { inlineData[resourceId] = it }
            resourceId
        }

    private companion object {
        const val MAX_LINKED_RESOURCES = 2_000
    }
}
