package mihon.entry.interactions.book.prose

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CancellationException
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
import mihon.entry.interactions.book.BookPublicationSession
import mihon.entry.interactions.book.BookReaderRequest
import org.jsoup.Jsoup
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/** Built-in reader processor for one source-normalized prose chapter. */
internal class HtmlProseChapterProcessor : BookProcessor {
    override val id: String = "builtin.html.prose-chapter"
    override val displayName: String = "Prose chapter reader"

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
                opened.stream.readBounded(MAX_HTML_RESOURCE_BYTES)
            }
            val bodyHtml = sanitize(bytes)
            if (Jsoup.parseBodyFragment(bodyHtml).text().isBlank()) {
                return failure(BookFailureReason.MALFORMED_CONTENT, "The prose chapter contains no readable text")
            }
            BookOpenResult.Success(
                HtmlProseChapterSession(
                    publicationId = content.publicationId,
                    revision = metadata.revision ?: content.revision,
                    resource = metadata,
                    bodyHtml = bodyHtml,
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

    private fun failure(reason: BookFailureReason, message: String): BookOpenResult.Failure =
        BookOpenResult.Failure(BookFailure(reason, message))

    private fun contentFailure(message: String): BookOpenResult.Failure =
        failure(BookFailureReason.CONTENT_UNAVAILABLE, message)

    private companion object {
        const val MAX_HTML_RESOURCE_BYTES = 4 * 1024 * 1024
    }
}

internal class HtmlProseChapterSession(
    publicationId: String,
    revision: String,
    resource: BookContentResource,
    val bodyHtml: String,
) : BookPublicationSession {
    val resourceId: String = resource.id

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

    override fun close() = Unit
}

private fun sanitize(bytes: ByteArray): String {
    val parsed = Jsoup.parse(bytes.inputStream(), null, "")
    val cleaned = Cleaner(PROSE_SAFELIST).clean(parsed)
    cleaned.select("a[href]").forEach { link ->
        if (!link.attr("href").startsWith("#")) link.removeAttr("href")
    }
    cleaned.outputSettings()
        .charset(StandardCharsets.UTF_8)
        .prettyPrint(false)
    return cleaned.body().html()
}

private fun java.io.InputStream.readBounded(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(maxBytes, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
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

internal const val HTML_MEDIA_TYPE = "text/html"
internal const val PROSE_CHAPTER_PROFILE = "prose-chapter"
private const val XHTML_MEDIA_TYPE = "application/xhtml+xml"

private val PROSE_SAFELIST = Safelist.none()
    .addTags(
        "a", "article", "aside", "b", "blockquote", "br", "caption", "cite", "code", "col", "colgroup", "dd",
        "div", "dl", "dt", "em", "figcaption", "figure", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "i",
        "li", "ol", "p", "pre", "q", "s", "section", "small", "span", "strike", "strong", "sub", "sup",
        "table", "tbody", "td", "tfoot", "th", "thead", "tr", "u", "ul",
    )
    .addAttributes(":all", "id", "lang", "dir")
    .addAttributes("a", "href", "name", "title")
    .addAttributes("col", "span")
    .addAttributes("colgroup", "span")
    .addAttributes("ol", "start", "type")
    .addAttributes("td", "colspan", "rowspan")
    .addAttributes("th", "colspan", "rowspan", "scope")
