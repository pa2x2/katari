package mihon.entry.interactions.book.format.html.prosechapter.preparation

import eu.kanade.tachiyomi.network.HttpException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookPublication
import mihon.book.api.BookReadingDirection
import mihon.book.api.BookResource
import mihon.book.api.document.BookDocument
import mihon.book.api.document.BookDocumentPublicationModel
import mihon.entry.interactions.book.content.BookContentSession
import mihon.entry.interactions.book.content.BookContentSessionResourceLoader
import mihon.entry.interactions.book.content.isReadableBookResource
import mihon.entry.interactions.book.content.normalizedBookContentLanguages
import mihon.entry.interactions.book.content.normalizedBookMediaType
import mihon.entry.interactions.book.content.readBoundedBookResource
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseChapterContract
import mihon.entry.interactions.book.format.html.prosechapter.HtmlProseLimitExceededException
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import mihon.entry.interactions.book.preparation.BookContentPreparer
import mihon.entry.interactions.book.preparation.BookPreparationResult
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

internal class HtmlProseChapterPreparer : BookContentPreparer {
    override val id = "html.prose-chapter"
    override val outputModel = BookDocumentPublicationModel.DESCRIPTOR

    override fun supports(descriptor: mihon.book.api.BookContentDescriptor): Boolean =
        HtmlProseChapterContract.supports(descriptor)

    override suspend fun prepare(content: BookContentSession): BookPreparationResult {
        if (!supports(content.descriptor)) {
            return failure(BookFailureReason.FORMAT_UNSUPPORTED, "Unsupported prose chapter descriptor")
        }
        val resourceId = content.primaryResourceIds.singleOrNull()
            ?: return failure(
                BookFailureReason.MALFORMED_CONTENT,
                "A prose chapter must declare exactly one primary HTML resource",
            )
        return try {
            val resource = content.getResource(resourceId).getOrElse { error ->
                return failure(
                    BookFailureReason.CONTENT_UNAVAILABLE,
                    error.message ?: "The prose chapter resource is unavailable",
                    canRetry = error.isTransientBookAccessFailure(),
                )
            }
            if (!resource.isReadableBookResource()) {
                return failure(BookFailureReason.CONTENT_UNAVAILABLE, "The prose chapter resource is not readable")
            }
            if (resource.mediaType.normalizedBookMediaType()?.let { it != HtmlProseChapterContract.FORMAT } == true) {
                return failure(BookFailureReason.FORMAT_UNSUPPORTED, "The primary resource is not text/html")
            }
            if (resource.size?.let { it > HtmlProseChapterContract.MAX_RAW_BYTES } == true) {
                return failure(BookFailureReason.MALFORMED_CONTENT, "The prose chapter exceeds its byte limit")
            }
            val prepared = content.openResource(resourceId).getOrElse { error ->
                return failure(
                    BookFailureReason.CONTENT_UNAVAILABLE,
                    error.message ?: "The prose chapter resource could not be opened",
                    canRetry = error.isTransientBookAccessFailure(),
                )
            }.use { opened ->
                require(opened.metadata.id == resourceId) { "The opened prose resource identity changed" }
                require(
                    opened.metadata.mediaType.normalizedBookMediaType()
                        ?.let { it == HtmlProseChapterContract.FORMAT } != false,
                ) { "The opened prose resource is not text/html" }
                val bytes = withContext(Dispatchers.IO) {
                    opened.stream.readBoundedBookResource(HtmlProseChapterContract.MAX_RAW_BYTES)
                }
                withContext(Dispatchers.Default) {
                    val body = HtmlProseSanitizer.sanitize(bytes)
                    PreparedHtmlProseChapter(
                        document = HtmlProseDocumentParser().parse(
                            resourceId,
                            resource.revision ?: content.revision,
                            body,
                        ),
                        readingDirection = if (body.attr("dir").equals("rtl", true)) {
                            BookReadingDirection.RIGHT_TO_LEFT
                        } else {
                            BookReadingDirection.LEFT_TO_RIGHT
                        },
                        language = body.attr("lang").takeIf(String::isNotBlank),
                    )
                }
            }
            val publication = BookPublication(
                id = content.publicationId,
                revision = content.revision,
                title = resource.title,
                languages = (listOf(prepared.language) + content.languages)
                    .filterNotNull()
                    .normalizedBookContentLanguages(),
                readingDirection = prepared.readingDirection,
                readingOrder = listOf(BookResource(resource.id, HtmlProseChapterContract.FORMAT, resource.title)),
                navigation = listOf(
                    BookNavigationItem(
                        title = resource.title,
                        target = BookLocator(resourceId = resource.id, progression = 0.0),
                    ),
                ),
            )
            BookPreparationResult.Success(
                PreparedBookDocumentPublication(
                    publication = publication,
                    model = BookDocumentPublicationModel(listOf(prepared.document)),
                    resourceLoader = BookContentSessionResourceLoader(content),
                ),
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: HtmlProseLimitExceededException) {
            failure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "The prose chapter exceeds a safety limit")
        } catch (error: IllegalArgumentException) {
            failure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "The prose chapter is malformed")
        } catch (error: Exception) {
            failure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "The prose chapter could not be prepared")
        }
    }

    private fun failure(
        reason: BookFailureReason,
        message: String,
        canRetry: Boolean = false,
    ): BookPreparationResult.Failure = BookPreparationResult.Failure(BookFailure(reason, message), canRetry)
}

private fun Throwable.isTransientBookAccessFailure(): Boolean {
    return generateSequence(this) { it.cause }.any { error ->
        when (error) {
            is SocketTimeoutException,
            is UnknownHostException,
            is ConnectException,
            is NoRouteToHostException,
            is SocketException,
            -> true
            is HttpException -> error.code in RETRYABLE_HTTP_STATUS_CODES
            else -> false
        }
    }
}

private val RETRYABLE_HTTP_STATUS_CODES = setOf(408, 425, 429, 500, 502, 503, 504, 520, 521, 522, 523, 524)

private data class PreparedHtmlProseChapter(
    val document: BookDocument,
    val readingDirection: BookReadingDirection,
    val language: String?,
)
