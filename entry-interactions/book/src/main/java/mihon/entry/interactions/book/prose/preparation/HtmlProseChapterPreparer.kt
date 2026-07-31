package mihon.entry.interactions.book.prose

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.entry.interactions.book.BookContentPreparer
import mihon.entry.interactions.book.BookContentSession
import mihon.entry.interactions.book.BookContentSessionResourceLoader
import mihon.entry.interactions.book.BookPreparationResult
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.isReadableBookResource
import mihon.entry.interactions.book.readBoundedBookResource

/** App-owned preparation of source-normalized HTML into the canonical structured-prose model. */
internal class HtmlProseChapterPreparer : BookContentPreparer {
    override val id: String = "builtin.html.prose-chapter"
    override val outputModel = BookDocumentPublicationModel.DESCRIPTOR

    override fun supports(descriptor: BookContentDescriptor): Boolean =
        descriptor.format == HTML_MEDIA_TYPE &&
            descriptor.profile == PROSE_CHAPTER_PROFILE &&
            descriptor.protection == "none"

    override suspend fun prepare(content: BookContentSession): BookPreparationResult {
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
        if (!metadata.isReadableBookResource()) {
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
                    opened.stream.readBoundedBookResource(MAX_HTML_RESOURCE_BYTES)
                }
            }
            val document = withContext(Dispatchers.Default) {
                parseHtmlBookDocument(
                    resourceId = resourceId,
                    revision = metadata.revision ?: content.revision,
                    body = sanitizeProseDocument(bytes),
                )
            }
            BookPreparationResult.Success(
                PreparedBookDocumentPublication(
                    publicationId = content.publicationId,
                    revision = metadata.revision ?: content.revision,
                    resource = metadata,
                    model = BookDocumentPublicationModel(listOf(document)),
                    resourceLoader = BookContentSessionResourceLoader(content),
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

    private fun failure(reason: BookFailureReason, message: String): BookPreparationResult.Failure =
        BookPreparationResult.Failure(BookFailure(reason, message))

    private fun contentFailure(message: String): BookPreparationResult.Failure =
        failure(BookFailureReason.CONTENT_UNAVAILABLE, message)

    private companion object {
        const val MAX_HTML_RESOURCE_BYTES = 4 * 1024 * 1024
    }
}
