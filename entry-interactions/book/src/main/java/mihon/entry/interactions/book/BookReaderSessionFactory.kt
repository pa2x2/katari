package mihon.entry.interactions.book

import android.content.Context
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryHttpSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.CancellationException
import mihon.book.api.BookContentDescriptor
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.book.api.BookLocator
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadCleanup
import mihon.entry.interactions.book.download.BookDownloadPackageKey
import mihon.entry.interactions.book.download.DownloadedBookContentSession
import mihon.entry.interactions.book.download.VerifiedBookDownloadPackage
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
    private val materializationStore: BookMaterializationStore,
    private val downloadCache: BookDownloadCache,
    private val downloadCleanup: BookDownloadCleanup? = null,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun open(
        context: Context,
        request: BookReaderRequest,
        processorId: String,
    ): BookReaderOpenResult {
        return when (val prepared = prepare(request)) {
            is BookReaderPrepareResult.Failure -> BookReaderOpenResult.Failure(prepared.failure)
            is BookReaderPrepareResult.Success -> openPrepared(context, prepared.request, processorId)
        }
    }

    suspend fun prepare(request: BookReaderRequest): BookReaderPrepareResult {
        val visibleEntry = entryRepository.getEntryById(request.entryId)
            ?: return prepareFailure(BookFailureReason.CONTENT_UNAVAILABLE, "The book entry no longer exists.")
        if (visibleEntry.type != EntryType.BOOK) {
            return prepareFailure(BookFailureReason.MALFORMED_CONTENT, "The selected entry is not a book.")
        }
        val chapter = entryChapterRepository.getChapterById(request.chapterId)
            ?: return prepareFailure(BookFailureReason.CONTENT_UNAVAILABLE, "The selected book item no longer exists.")
        val owner = entryRepository.getEntryById(chapter.entryId)
            ?: return prepareFailure(BookFailureReason.CONTENT_UNAVAILABLE, "The book item owner no longer exists.")
        if (owner.type != EntryType.BOOK) {
            return prepareFailure(BookFailureReason.MALFORMED_CONTENT, "The selected item does not belong to a book.")
        }
        resolveDownloadedContent(owner, chapter)?.let { downloaded ->
            return preparedSuccess(request, visibleEntry, owner, chapter, downloaded)
        }
        val source = sourceManager.get(owner.source)
            ?: return prepareFailure(BookFailureReason.CONTENT_UNAVAILABLE, "The book source is not available.")
        val media = try {
            source.getMedia(chapter.toSEntryChapter()) as? EntryMedia.Book
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return prepareFailure(
                BookFailureReason.CONTENT_UNAVAILABLE,
                error.message ?: "The source could not resolve this book item.",
            )
        } ?: return prepareFailure(
            BookFailureReason.MALFORMED_CONTENT,
            "The source returned a different content type for this book item.",
        )
        return preparedSuccess(
            request = request,
            visibleEntry = visibleEntry,
            owner = owner,
            chapter = chapter,
            content = PreparedBookContent.Source(source, media),
        )
    }

    suspend fun openPrepared(
        context: Context,
        prepared: PreparedBookReaderRequest,
        processorId: String,
    ): BookReaderOpenResult {
        val processor = processorRegistry.get(processorId)
            ?: return failure(BookFailureReason.PROCESSOR_UNAVAILABLE, "The selected book reader is unavailable.")
        val visibleEntry = prepared.visibleEntry
        val owner = prepared.owner
        val chapter = prepared.chapter
        val content = prepared.content
        if (!processor.supports(content.descriptor)) {
            return failure(BookFailureReason.FORMAT_UNSUPPORTED, "The selected reader does not support this content.")
        }

        val progressIdentity = try {
            content.progressIdentity(chapter.id)
        } catch (error: IllegalStateException) {
            return failure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "The book resource is ambiguous.")
        }
        val contentSession = content.createSession(context, owner)
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
                            historySourceId = owner.source,
                            chapter = chapter,
                            progressIdentity = progressIdentity,
                            contentSession = contentSession,
                            publicationSession = opened.session,
                            initialLocator = initialLocator,
                            entryProgressRepository = entryProgressRepository,
                            historyRepository = historyRepository,
                            incognitoState = incognitoState,
                            downloadCleanup = downloadCleanup,
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

    private fun prepareFailure(reason: BookFailureReason, message: String): BookReaderPrepareResult.Failure {
        return BookReaderPrepareResult.Failure(BookFailure(reason, message))
    }

    private suspend fun resolveDownloadedContent(
        owner: Entry,
        chapter: EntryChapter,
    ): PreparedBookContent.Downloaded? {
        val packageKey = try {
            BookDownloadPackageKey(owner.source, owner.url, chapter.url)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return try {
            downloadCache.ensureInitialized()
            downloadCache.get(packageKey)?.let(PreparedBookContent::Downloaded)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
    }

    private fun preparedSuccess(
        request: BookReaderRequest,
        visibleEntry: Entry,
        owner: Entry,
        chapter: EntryChapter,
        content: PreparedBookContent,
    ): BookReaderPrepareResult.Success = BookReaderPrepareResult.Success(
        PreparedBookReaderRequest(
            request = request,
            visibleEntry = visibleEntry,
            owner = owner,
            chapter = chapter,
            content = content,
        ),
    )

    private fun PreparedBookContent.createSession(context: Context, owner: Entry): BookContentSession = when (this) {
        is PreparedBookContent.Downloaded -> DownloadedBookContentSession(download, materializationStore)
        is PreparedBookContent.Source -> SourceBookContentSession(
            source = source,
            entry = owner,
            media = media,
            externalResolver = AndroidBookExternalResourceResolver(
                context = context.applicationContext,
                httpClient = (source as? EntryHttpSource)?.client ?: networkHelper.client,
            ),
            materializationStore = materializationStore,
        )
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

internal data class PreparedBookReaderRequest(
    val request: BookReaderRequest,
    val visibleEntry: Entry,
    val owner: Entry,
    val chapter: EntryChapter,
    val content: PreparedBookContent,
)

internal sealed interface PreparedBookContent {
    val descriptor: BookContentDescriptor

    fun progressIdentity(chapterId: Long): BookProgressIdentity

    data class Source(
        val source: UnifiedSource,
        val media: EntryMedia.Book,
    ) : PreparedBookContent {
        override val descriptor: BookContentDescriptor = media.descriptor

        override fun progressIdentity(chapterId: Long): BookProgressIdentity = media.progressIdentity(chapterId)
    }

    data class Downloaded(
        val download: VerifiedBookDownloadPackage,
    ) : PreparedBookContent {
        override val descriptor: BookContentDescriptor = download.manifest.descriptor

        override fun progressIdentity(chapterId: Long): BookProgressIdentity =
            download.manifest.progressIdentity(chapterId)
    }
}

internal sealed interface BookReaderPrepareResult {
    data class Success(val request: PreparedBookReaderRequest) : BookReaderPrepareResult
    data class Failure(val failure: BookFailure) : BookReaderPrepareResult
}

internal sealed interface BookReaderOpenResult {
    data class Success(val session: OpenedBookReaderSession) : BookReaderOpenResult
    data class Failure(val failure: BookFailure) : BookReaderOpenResult
}

internal class OpenedBookReaderSession(
    val entry: Entry,
    private val historySourceId: Long,
    val chapter: EntryChapter,
    private val progressIdentity: BookProgressIdentity,
    contentSession: BookContentSession,
    val publicationSession: BookPublicationSession,
    val initialLocator: BookLocator?,
    private val entryProgressRepository: EntryProgressRepository,
    private val historyRepository: HistoryRepository,
    private val incognitoState: mihon.entry.interactions.EntryReaderIncognitoState,
    private val downloadCleanup: BookDownloadCleanup? = null,
    private val now: () -> Long,
) : AutoCloseable {
    private val closeStack = BookSessionCloseStack().apply {
        own(contentSession)
        own(publicationSession)
    }

    suspend fun saveLocation(locator: BookLocator, completed: Boolean = false) {
        if (incognitoState.isIncognito(historySourceId)) return
        val timestamp = now()
        val current = entryProgressRepository.get(
            chapter.entryId,
            progressIdentity.contentKey,
            progressIdentity.resourceKey,
        )
        val shouldBeCompleted = current?.completed == true || chapter.read || completed
        val completedNow = shouldBeCompleted && current?.completed != true
        entryProgressRepository.mergeAndSyncChild(
            current?.copy(
                chapterId = chapter.id,
                resourceRevision = progressIdentity.resourceRevision,
                locator = BookProgressLocatorCodec.encode(locator, current.locator.extensions),
                locatorUpdatedAt = timestamp,
                completed = shouldBeCompleted,
                completionUpdatedAt = if (completedNow) timestamp else current.completionUpdatedAt,
            ) ?: EntryProgressState(
                entryId = chapter.entryId,
                chapterId = chapter.id,
                contentKey = progressIdentity.contentKey,
                resourceKey = progressIdentity.resourceKey,
                resourceRevision = progressIdentity.resourceRevision,
                locator = BookProgressLocatorCodec.encode(locator),
                locatorUpdatedAt = timestamp,
                completed = shouldBeCompleted,
                completionUpdatedAt = if (shouldBeCompleted) timestamp else 0L,
            ),
        )
        if (completed && current?.completed != true) {
            downloadCleanup?.afterReaderCompleted(entry, chapter)
        }
    }

    suspend fun recordHistory(sessionReadDuration: Long) {
        if (sessionReadDuration <= 0L || incognitoState.isIncognito(historySourceId)) return
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
