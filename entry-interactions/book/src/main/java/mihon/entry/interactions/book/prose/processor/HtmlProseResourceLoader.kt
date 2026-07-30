package mihon.entry.interactions.book.prose

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.book.api.BookContentResource
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCapability
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookResourceRequirement
import mihon.entry.interactions.book.document.model.BookDocument
import mihon.entry.interactions.book.document.model.BookDocumentBlock
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.reader.BookDocumentBinaryResource
import mihon.entry.interactions.book.document.reader.BookDocumentResourceLoader
import mihon.entry.interactions.book.document.resource.PROSE_FONT_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import java.io.ByteArrayOutputStream

/** Built-in reader processor for one source-normalized prose chapter. */
internal fun BookDocument.resourceRequirements(): Map<String, BookResourceRequirement> = buildMap {
    fun register(resourceId: String, requirement: BookResourceRequirement) {
        val existing = get(resourceId)
        require(existing == null || existing == requirement) {
            "Prose resource $resourceId is used with incompatible validation constraints"
        }
        put(resourceId, requirement)
    }

    fun collect(blocks: List<BookDocumentBlock>) {
        blocks.forEach { block ->
            (block.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId?.let { resourceId ->
                register(resourceId, PROSE_FONT_RESOURCE_REQUIREMENT)
            }
            block.inlineStyles.forEach { inline ->
                (inline.style.fontFamily as? BookDocumentFontFamily.Resource)?.resourceId?.let { resourceId ->
                    register(resourceId, PROSE_FONT_RESOURCE_REQUIREMENT)
                }
            }
            when (val content = block.content) {
                is BookDocumentBlockContent.Figure ->
                    register(content.image.resourceId, PROSE_IMAGE_RESOURCE_REQUIREMENT)
                is BookDocumentBlockContent.Disclosure -> collect(content.body)
                else -> Unit
            }
        }
    }
    collect(blocks)
}

internal class HtmlProseResourceLoader(
    private val content: BookContentSession,
) : BookDocumentResourceLoader {
    private val cacheLock = Any()
    private val cache = LinkedHashMap<String, BookDocumentBinaryResource>(4, 0.75f, true)
    private var cachedBytes = 0

    override suspend fun load(
        resourceId: String,
        acceptedMediaTypes: Set<String>,
        maxBytes: Int,
    ): Result<BookDocumentBinaryResource> {
        return try {
            require(maxBytes in 1..MAX_SUBORDINATE_RESOURCE_BYTES) { "Invalid prose resource byte limit" }
            synchronized(cacheLock) {
                cache[resourceId]?.takeIf { resource ->
                    resource.bytes.size <= maxBytes && resource.mediaType in acceptedMediaTypes
                }
            }?.let { return Result.success(it) }

            val metadata = content.getResource(resourceId).getOrThrow()
            require(metadata.isReadable()) { "Prose resource $resourceId is not readable" }
            val mediaType = metadata.mediaType.normalizedMediaType()
            require(mediaType in acceptedMediaTypes) {
                "Prose resource $resourceId has unsupported media type ${metadata.mediaType}"
            }
            metadata.size?.let { size ->
                require(size <= maxBytes) { "Prose resource $resourceId exceeds its byte limit" }
            }
            val bytes = content.openResource(resourceId).getOrThrow().use { opened ->
                require(opened.metadata.mediaType.normalizedMediaType() in acceptedMediaTypes) {
                    "Prose resource $resourceId returned an unsupported media type ${opened.metadata.mediaType}"
                }
                withContext(Dispatchers.IO) {
                    opened.stream.readBounded(maxBytes)
                }
            }
            require(bytes.isNotEmpty()) { "Prose resource $resourceId is empty" }
            Result.success(BookDocumentBinaryResource(resourceId, mediaType, bytes).also(::storeInCache))
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private fun storeInCache(resource: BookDocumentBinaryResource) {
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

internal suspend fun java.io.InputStream.readBounded(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        currentCoroutineContext().ensureActive()
        val read = read(buffer)
        if (read < 0) break
        total += read
        require(total <= maxBytes) { "The selected prose chapter is too large" }
        output.write(buffer, 0, read)
    }
    return output.toByteArray()
}

internal fun BookContentResource.isHtmlResource(): Boolean = when (
    mediaType?.substringBefore(';')?.trim()?.lowercase()
) {
    null, HTML_MEDIA_TYPE, XHTML_MEDIA_TYPE -> true
    else -> false
}

internal fun BookContentResource.isReadable(): Boolean =
    BookResourceCapability.STREAM in capabilities &&
        (availability == BookResourceAvailability.UNKNOWN || availability == BookResourceAvailability.AVAILABLE)

internal fun String?.normalizedMediaType(): String? = this?.substringBefore(';')?.trim()?.lowercase()

internal const val HTML_MEDIA_TYPE = "text/html"
internal const val PROSE_CHAPTER_PROFILE = "prose-chapter"
internal const val XHTML_MEDIA_TYPE = "application/xhtml+xml"
