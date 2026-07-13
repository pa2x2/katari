package mihon.entry.interactions.book

import android.content.Context
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryHttpSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CancellationException
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.book.api.BookLocator
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.source.service.SourceManager
import java.util.Date

internal class BookReaderSessionFactory(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryProgressRepository: EntryProgressRepository,
    private val historyRepository: HistoryRepository,
    private val sourceManager: SourceManager,
    private val processorRegistry: BookProcessorRegistry,
    private val networkHelper: NetworkHelper,
    private val incognitoState: mihon.entry.interactions.EntryReaderIncognitoState,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun open(
        context: Context,
        request: BookReaderRequest,
        processorId: String,
    ): BookReaderOpenResult {
        val processor = processorRegistry.get(processorId)
            ?: return failure(BookFailureReason.PROCESSOR_UNAVAILABLE, "The selected book reader is unavailable.")
        val visibleEntry = entryRepository.getEntryById(request.entryId)
            ?: return failure(BookFailureReason.CONTENT_UNAVAILABLE, "The book entry no longer exists.")
        if (visibleEntry.type != EntryType.BOOK) {
            return failure(BookFailureReason.MALFORMED_CONTENT, "The selected entry is not a book.")
        }
        val chapter = entryChapterRepository.getChapterById(request.chapterId)
            ?: return failure(BookFailureReason.CONTENT_UNAVAILABLE, "The selected book item no longer exists.")
        val owner = entryRepository.getEntryById(chapter.entryId)
            ?: return failure(BookFailureReason.CONTENT_UNAVAILABLE, "The book item owner no longer exists.")
        if (owner.type != EntryType.BOOK) {
            return failure(BookFailureReason.MALFORMED_CONTENT, "The selected item does not belong to a book.")
        }
        val source = sourceManager.get(owner.source)
            ?: return failure(BookFailureReason.CONTENT_UNAVAILABLE, "The book source is not available.")
        val media = try {
            source.getMedia(chapter.toSEntryChapter()) as? EntryMedia.Book
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return failure(
                BookFailureReason.CONTENT_UNAVAILABLE,
                error.message ?: "The source could not resolve this book item.",
            )
        } ?: return failure(
            BookFailureReason.MALFORMED_CONTENT,
            "The source returned a different content type for this book item.",
        )
        if (!processor.supports(media.descriptor)) {
            return failure(BookFailureReason.FORMAT_UNSUPPORTED, "The selected reader does not support this content.")
        }

        val progressIdentity = try {
            media.progressIdentity(chapter.id)
        } catch (error: IllegalStateException) {
            return failure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "The book resource is ambiguous.")
        }
        val contentSession = SourceBookContentSession(
            source = source,
            entry = owner,
            media = media,
            externalResolver = AndroidBookExternalResourceResolver(
                context = context.applicationContext,
                httpClient = (source as? EntryHttpSource)?.client ?: networkHelper.client,
            ),
            materializationDirectory = context.cacheDir.resolve("book-materialized"),
        )
        val opened = try {
            processor.open(contentSession)
        } catch (error: CancellationException) {
            closeAfterFailure(contentSession, cause = error)
            throw error
        } catch (error: Exception) {
            closeAfterFailure(contentSession, cause = error)
            return failure(
                BookFailureReason.MALFORMED_CONTENT,
                error.message ?: "The book reader could not open this content.",
            )
        }
        return when (opened) {
            is BookOpenResult.Failure -> {
                closeAfterFailure(contentSession)
                BookReaderOpenResult.Failure(opened.failure)
            }
            is BookOpenResult.Success -> {
                try {
                    val progress = entryProgressRepository.get(
                        chapter.entryId,
                        progressIdentity.contentKey,
                        progressIdentity.resourceKey,
                    )
                    val initialLocator = progress
                        ?.locator
                        ?.let(BookProgressLocatorCodec::decode)
                        ?.takeIf(opened.session::validate)
                    BookReaderOpenResult.Success(
                        OpenedBookReaderSession(
                            entry = visibleEntry,
                            chapter = chapter,
                            progressIdentity = progressIdentity,
                            contentSession = contentSession,
                            publicationSession = opened.session,
                            initialLocator = initialLocator,
                            entryProgressRepository = entryProgressRepository,
                            historyRepository = historyRepository,
                            incognitoState = incognitoState,
                            now = now,
                        ),
                    )
                } catch (error: CancellationException) {
                    closeAfterFailure(contentSession, opened.session, error)
                    throw error
                } catch (error: Exception) {
                    closeAfterFailure(contentSession, opened.session, error)
                    failure(
                        BookFailureReason.CONTENT_UNAVAILABLE,
                        error.message ?: "The saved book position could not be restored.",
                    )
                }
            }
        }
    }

    private fun failure(reason: BookFailureReason, message: String): BookReaderOpenResult.Failure {
        return BookReaderOpenResult.Failure(BookFailure(reason, message))
    }

    private fun closeAfterFailure(
        contentSession: BookContentSession,
        publicationSession: BookPublicationSession? = null,
        cause: Throwable? = null,
    ) {
        val closeStack = BookSessionCloseStack().apply {
            own(contentSession)
            publicationSession?.let(::own)
        }
        runCatching(closeStack::close).exceptionOrNull()?.let { closeError ->
            cause?.addSuppressed(closeError)
        }
    }
}

internal sealed interface BookReaderOpenResult {
    data class Success(val session: OpenedBookReaderSession) : BookReaderOpenResult
    data class Failure(val failure: BookFailure) : BookReaderOpenResult
}

internal class OpenedBookReaderSession(
    val entry: Entry,
    val chapter: EntryChapter,
    private val progressIdentity: BookProgressIdentity,
    contentSession: BookContentSession,
    val publicationSession: BookPublicationSession,
    val initialLocator: BookLocator?,
    private val entryProgressRepository: EntryProgressRepository,
    private val historyRepository: HistoryRepository,
    private val incognitoState: mihon.entry.interactions.EntryReaderIncognitoState,
    private val now: () -> Long,
) : AutoCloseable {
    private val closeStack = BookSessionCloseStack().apply {
        own(contentSession)
        own(publicationSession)
    }

    suspend fun saveLocation(locator: BookLocator) {
        val timestamp = now()
        val current = entryProgressRepository.get(
            chapter.entryId,
            progressIdentity.contentKey,
            progressIdentity.resourceKey,
        )
        entryProgressRepository.mergeAndSyncChild(
            current?.copy(
                chapterId = chapter.id,
                resourceRevision = progressIdentity.resourceRevision,
                locator = BookProgressLocatorCodec.encode(locator, current.locator.extensions),
                locatorUpdatedAt = timestamp,
            ) ?: EntryProgressState(
                entryId = chapter.entryId,
                chapterId = chapter.id,
                contentKey = progressIdentity.contentKey,
                resourceKey = progressIdentity.resourceKey,
                resourceRevision = progressIdentity.resourceRevision,
                locator = BookProgressLocatorCodec.encode(locator),
                locatorUpdatedAt = timestamp,
            ),
        )
    }

    suspend fun recordHistory(sessionReadDuration: Long) {
        if (sessionReadDuration <= 0L || incognitoState.isIncognito(entry.source)) return
        historyRepository.upsertHistory(
            HistoryUpdate(
                chapterId = chapter.id,
                readAt = Date(now()),
                sessionReadDuration = sessionReadDuration,
            ),
        )
    }

    override fun close() = closeStack.close()
}
