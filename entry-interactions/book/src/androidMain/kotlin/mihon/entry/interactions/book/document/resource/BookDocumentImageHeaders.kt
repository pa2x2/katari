package mihon.entry.interactions.book.document.resource

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Image headers per live publication, without retaining closed resource owners. A figure that has been on
 * screen once, or whose chapter has been read ahead, lays out at its final size when it is composed again
 * instead of growing when its bitmap arrives, which would move everything below it.
 */
internal object BookDocumentImageHeaders {
    private val publications = WeakHashMap<BookPublicationResourceLoader, MutableMap<String, ProseImageHeader>>()

    fun cached(loader: BookPublicationResourceLoader, resourceId: String): ProseImageHeader? =
        headers(loader)[resourceId]

    fun record(loader: BookPublicationResourceLoader, resourceId: String, header: ProseImageHeader) {
        headers(loader)[resourceId] = header
    }

    /** Null when the image can't be loaded or read; failures are not kept, since access can change. */
    suspend fun read(loader: BookPublicationResourceLoader, resourceId: String): ProseImageHeader? {
        cached(loader, resourceId)?.let { return it }
        return try {
            val resource = loader.load(
                resourceId = resourceId,
                acceptedMediaTypes = PROSE_IMAGE_RESOURCE_REQUIREMENT.acceptedMediaTypes,
                maxBytes = PROSE_IMAGE_RESOURCE_REQUIREMENT.maxBytes,
            ).getOrThrow()
            withContext(Dispatchers.Default) { readProseImageHeader(resource.bytes, resource.mediaType) }
                .also { record(loader, resourceId, it) }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    private fun headers(loader: BookPublicationResourceLoader) =
        synchronized(publications) { publications.getOrPut(loader) { ConcurrentHashMap() } }
}
