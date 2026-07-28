package mihon.entry.interactions.book.prose

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookContentResource
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookPublication
import mihon.book.api.BookReadingDirection
import mihon.book.api.BookResource
import mihon.book.api.BookResourceAvailability
import mihon.book.api.BookResourceCapability
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookOpenResult
import mihon.entry.interactions.book.BookProcessor
import mihon.entry.interactions.book.BookPublicationResourceDependencies
import mihon.entry.interactions.book.BookPublicationSession
import mihon.entry.interactions.book.BookReaderRequest
import mihon.entry.interactions.book.BookResourceRequirement
import mihon.entry.interactions.book.document.model.BookDocument
import mihon.entry.interactions.book.document.model.BookDocumentBlock
import mihon.entry.interactions.book.document.model.BookDocumentBlockContent
import mihon.entry.interactions.book.document.model.BookDocumentFontFamily
import mihon.entry.interactions.book.document.reader.BookDocumentBinaryResource
import mihon.entry.interactions.book.document.reader.BookDocumentResourceLoader
import mihon.entry.interactions.book.document.render.PreparedBookDocument
import mihon.entry.interactions.book.document.resource.PROSE_FONT_RESOURCE_REQUIREMENT
import mihon.entry.interactions.book.document.resource.PROSE_IMAGE_RESOURCE_REQUIREMENT
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.viewer.settings.StandardReaderCapabilities
import java.io.ByteArrayOutputStream

/** Built-in reader processor for one source-normalized prose chapter. */
internal class HtmlProseChapterProcessor : BookProcessor {
    override val id: String = "builtin.html.prose-chapter"
    override val displayName: String = "Prose chapter reader"
    override val viewerSettingsSurfaceId = HtmlProseSettingsProvider.PROVIDER_ID
    override val potentialReaderCapabilities = TEXT_SELECTION_CAPABILITIES

    override fun supports(descriptor: BookContentDescriptor): Boolean =
        descriptor.format == HTML_MEDIA_TYPE &&
            descriptor.profile == PROSE_CHAPTER_PROFILE &&
            descriptor.protection == "none"

    override fun createReaderIntent(
        context: Context,
        request: BookReaderRequest,
        sessionToken: String,
    ): Intent = HtmlProseChapterReaderActivity.newIntent(context, request, id, sessionToken)

    override suspend fun open(content: BookContentSession): BookOpenResult {
        if (!supports(content.descriptor)) {
            return failure(BookFailureReason.FORMAT_UNSUPPORTED, "Unsupported prose chapter descriptor")
        }

        val resourceId = content.primaryResourceIds.singleOrNull()
            ?: return contentFailure("A prose chapter must identify exactly one primary resource")
        val metadata = content.getResource(resourceId).getOrElse {
            return contentFailure(it.message ?: "Unable to resolve the prose chapter")
        }
        if (!metadata.isHtmlResource()) {
            return failure(BookFailureReason.FORMAT_UNSUPPORTED, "The selected prose chapter is not HTML")
        }
        if (!metadata.isReadable()) {
            return contentFailure("The selected prose chapter is not currently readable (${metadata.availability})")
        }
        metadata.size?.let {
            if (it > MAX_HTML_RESOURCE_BYTES) return contentFailure("The selected prose chapter is too large")
        }

        return try {
            val bytes = content.openResource(resourceId).getOrElse {
                return contentFailure(it.message ?: "Unable to open the prose chapter")
            }.use { opened ->
                withContext(Dispatchers.IO) {
                    opened.stream.readBounded(MAX_HTML_RESOURCE_BYTES)
                }
            }
            val document = withContext(Dispatchers.Default) {
                prepareHtmlBookDocument(
                    resourceId = resourceId,
                    revision = metadata.revision ?: content.revision,
                    body = sanitizeProseDocument(bytes),
                )
            }
            BookOpenResult.Success(
                HtmlProseChapterSession(
                    publicationId = content.publicationId,
                    revision = metadata.revision ?: content.revision,
                    resource = metadata,
                    document = document,
                    content = content,
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            failure(
                BookFailureReason.MALFORMED_CONTENT,
                error.message ?: "Unable to prepare the prose chapter",
            )
        }
    }

    override fun readerCapabilities(session: BookPublicationSession) =
        if (session is HtmlProseChapterSession) TEXT_SELECTION_CAPABILITIES else emptySet()

    private fun failure(reason: BookFailureReason, message: String): BookOpenResult.Failure =
        BookOpenResult.Failure(BookFailure(reason, message))

    private fun contentFailure(message: String): BookOpenResult.Failure =
        failure(BookFailureReason.CONTENT_UNAVAILABLE, message)

    private companion object {
        const val MAX_HTML_RESOURCE_BYTES = 4 * 1024 * 1024
        val TEXT_SELECTION_CAPABILITIES = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
        )
    }
}

internal class HtmlProseChapterSession(
    publicationId: String,
    revision: String,
    resource: BookContentResource,
    val document: PreparedBookDocument,
    content: BookContentSession,
) : BookPublicationSession, BookPublicationResourceDependencies {
    val resourceId: String = resource.id
    val resourceLoader: BookDocumentResourceLoader = HtmlProseResourceLoader(content)
    override val requiredResourceIds: Set<String> = document.document.resourceIds
    override val resourceRequirements: Map<String, BookResourceRequirement> =
        document.document.resourceRequirements().also { requirements ->
            require(requirements.keys == requiredResourceIds) {
                "Every required prose resource must declare offline validation constraints"
            }
        }

    override val publication = BookPublication(
        id = publicationId,
        revision = revision,
        title = resource.title,
        languages = emptyList(),
        readingDirection = BookReadingDirection.LEFT_TO_RIGHT,
        readingOrder = listOf(
            BookResource(
                id = resource.id,
                mediaType = resource.mediaType,
                title = resource.title,
            ),
        ),
        navigation = listOf(
            BookNavigationItem(
                title = resource.title,
                target = BookLocator(resourceId = resource.id, progression = 0.0),
            ),
        ),
    )

    override fun validate(locator: BookLocator): Boolean =
        locator.resourceId == resourceId &&
            locator.progression?.let { it.isFinite() && it in 0.0..1.0 } != false

    override suspend fun reconcileMigratedLocator(locator: BookLocator): BookLocator? {
        if (validate(locator)) return locator
        val progression = locator.progression ?: locator.totalProgression ?: return null
        return BookLocator(
            resourceId = resourceId,
            progression = progression,
            totalProgression = locator.totalProgression,
            textContext = locator.textContext,
        )
    }

    override fun close() = Unit
}

private fun BookDocument.resourceRequirements(): Map<String, BookResourceRequirement> = buildMap {
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

private class HtmlProseResourceLoader(
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

private suspend fun java.io.InputStream.readBounded(maxBytes: Int): ByteArray {
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

private fun BookContentResource.isHtmlResource(): Boolean = when (
    mediaType?.substringBefore(';')?.trim()?.lowercase()
) {
    null, HTML_MEDIA_TYPE, XHTML_MEDIA_TYPE -> true
    else -> false
}

private fun BookContentResource.isReadable(): Boolean =
    BookResourceCapability.STREAM in capabilities &&
        (availability == BookResourceAvailability.UNKNOWN || availability == BookResourceAvailability.AVAILABLE)

private fun String?.normalizedMediaType(): String? = this?.substringBefore(';')?.trim()?.lowercase()

internal const val HTML_MEDIA_TYPE = "text/html"
internal const val PROSE_CHAPTER_PROFILE = "prose-chapter"
private const val XHTML_MEDIA_TYPE = "application/xhtml+xml"
