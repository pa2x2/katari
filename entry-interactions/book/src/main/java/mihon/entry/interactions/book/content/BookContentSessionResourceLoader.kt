package mihon.entry.interactions.book.content

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.book.api.BookContentResource
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCapability
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Bounded and cached renderer access to resources owned by a BOOK content session. */
internal class BookContentSessionResourceLoader(
    private val content: BookContentSession,
) : BookPublicationResourceLoader {
    private val cacheLock = Any()
    private val cache = LinkedHashMap<String, BookPublicationResource>(4, 0.75f, true)
    private var cachedBytes = 0

    override suspend fun load(
        resourceId: String,
        acceptedMediaTypes: Set<String>,
        maxBytes: Int,
    ): Result<BookPublicationResource> {
        return try {
            require(maxBytes in 1..MAX_SUBORDINATE_RESOURCE_BYTES) { "Invalid BOOK resource byte limit" }
            synchronized(cacheLock) {
                cache[resourceId]?.takeIf { resource ->
                    resource.bytes.size <= maxBytes && resource.mediaType in acceptedMediaTypes
                }
            }?.let { return Result.success(it) }

            val metadata = content.getResource(resourceId).getOrThrow()
            require(metadata.isReadableBookResource()) { "BOOK resource $resourceId is not readable" }
            val mediaType = metadata.mediaType.normalizedBookMediaType()
            require(mediaType in acceptedMediaTypes) {
                "BOOK resource $resourceId has unsupported media type ${metadata.mediaType}"
            }
            metadata.size?.let { size ->
                require(size <= maxBytes) { "BOOK resource $resourceId exceeds its byte limit" }
            }
            val bytes = content.openResource(resourceId).getOrThrow().use { opened ->
                require(opened.metadata.mediaType.normalizedBookMediaType() in acceptedMediaTypes) {
                    "BOOK resource $resourceId returned an unsupported media type ${opened.metadata.mediaType}"
                }
                withContext(Dispatchers.IO) {
                    opened.stream.readBoundedBookResource(maxBytes)
                }
            }
            require(bytes.isNotEmpty()) { "BOOK resource $resourceId is empty" }
            Result.success(BookPublicationResource(resourceId, mediaType, bytes).also(::storeInCache))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private fun storeInCache(resource: BookPublicationResource) {
        if (resource.bytes.size > MAX_RESOURCE_CACHE_BYTES) return
        synchronized(cacheLock) {
            cache.put(resource.resourceId, resource)?.let { previous ->
                cachedBytes -= previous.bytes.size
            }
            cachedBytes += resource.bytes.size
            while (cache.size > MAX_RESOURCE_CACHE_ENTRIES || cachedBytes > MAX_RESOURCE_CACHE_BYTES) {
                val oldest = cache.entries.firstOrNull() ?: break
                cache.remove(oldest.key)
                cachedBytes -= oldest.value.bytes.size
            }
        }
    }

    private companion object {
        const val MAX_SUBORDINATE_RESOURCE_BYTES = 16 * 1024 * 1024
        const val MAX_RESOURCE_CACHE_BYTES = 16 * 1024 * 1024
        const val MAX_RESOURCE_CACHE_ENTRIES = 8
    }
}

internal suspend fun InputStream.readBoundedBookResource(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        currentCoroutineContext().ensureActive()
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "BOOK resource exceeds its byte limit" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

internal fun BookContentResource.isReadableBookResource(): Boolean =
    BookResourceCapability.STREAM in capabilities &&
        (availability == BookResourceAvailability.UNKNOWN || availability == BookResourceAvailability.AVAILABLE)

internal fun String?.normalizedBookMediaType(): String? = this?.substringBefore(';')?.trim()?.lowercase()
