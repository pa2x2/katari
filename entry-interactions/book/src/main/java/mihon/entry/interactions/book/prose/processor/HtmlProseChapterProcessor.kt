package mihon.entry.interactions.book.prose

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookOpenResult
import mihon.entry.interactions.book.BookProcessor
import mihon.entry.interactions.book.BookPublicationSession
import mihon.entry.interactions.book.BookReaderRequest
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.viewer.settings.StandardReaderCapabilities

/** Built-in reader processor for one source-normalized prose chapter. */
internal class HtmlProseChapterProcessor : BookProcessor {
    override val id: String = "builtin.html.prose-chapter"
    override val displayName: String = "Prose chapter reader"
    override val viewerSettingsSurfaceId = HtmlProseSettingsProvider.PROVIDER_ID
    override val potentialReaderCapabilities = PROSE_READER_CAPABILITIES

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
        if (session is HtmlProseChapterSession) PROSE_READER_CAPABILITIES else emptySet()

    private fun failure(reason: BookFailureReason, message: String): BookOpenResult.Failure =
        BookOpenResult.Failure(BookFailure(reason, message))

    private fun contentFailure(message: String): BookOpenResult.Failure =
        failure(BookFailureReason.CONTENT_UNAVAILABLE, message)

    private companion object {
        const val MAX_HTML_RESOURCE_BYTES = 4 * 1024 * 1024
        val PROSE_READER_CAPABILITIES = setOf(
            StandardReaderCapabilities.StableTextSelection,
            StandardReaderCapabilities.SelectionAnchoring,
            StandardReaderCapabilities.NextChapterPreparation,
        )
    }
}
