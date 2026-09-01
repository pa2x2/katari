package mihon.entry.interactions.book.reader

import android.content.Context
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.EntryHttpSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CancellationException
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.entry.interactions.book.content.AndroidBookExternalResourceResolver
import mihon.entry.interactions.book.content.BookContentSession
import mihon.entry.interactions.book.content.BookMaterializationStore
import mihon.entry.interactions.book.content.SourceBookContentSession
import mihon.entry.interactions.book.download.BookDownloadCache
import mihon.entry.interactions.book.download.BookDownloadPackageKey
import mihon.entry.interactions.book.download.DownloadedBookContentSession
import mihon.entry.interactions.book.migration.isPendingBookMigration
import mihon.entry.interactions.book.preparation.BookContentPreparerRegistry
import mihon.entry.interactions.book.preparation.BookContentPreparerSelection
import mihon.entry.interactions.book.preparation.BookPreparationResult
import mihon.entry.interactions.book.preparation.PreparedBookPublication
import mihon.entry.interactions.book.processor.BookReaderProcessorRegistry
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.state.BOOK_PROGRESS_LOCATOR_KIND
import mihon.entry.interactions.book.state.BookProgressIdentity
import mihon.entry.interactions.book.state.BookProgressLocatorCodec
import mihon.entry.interactions.book.state.hasPartialBookProgress
import mihon.entry.interactions.media.EntryMediaSessionProcessor
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.model.EntryProgressState
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.source.service.SourceManager

internal class BookReaderSessionFactory(
    private val entryRepository: EntryRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val entryProgressRepository: EntryProgressRepository,
    private val sourceManager: SourceManager,
    private val preparerRegistry: BookContentPreparerRegistry,
    private val readerProcessorRegistry: BookReaderProcessorRegistry,
    private val networkHelper: NetworkHelper,
    private val materializationStore: BookMaterializationStore,
    private val downloadCache: BookDownloadCache,
    private val mediaSession: EntryMediaSessionProcessor,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun open(
        context: Context,
        request: BookReaderRequest,
        processorId: String,
    ): BookReaderOpenResult {
        return when (val prepared = prepare(request)) {
            is BookReaderPrepareResult.Failure -> BookReaderOpenResult.Failure(
                failure = prepared.failure,
                canRetry = prepared.canRetry,
            )
            is BookReaderPrepareResult.Success -> openPrepared(context, prepared.request, processorId)
        }
    }

    suspend fun prepare(request: BookReaderRequest): BookReaderPrepareResult {
        val visibleEntry = entryRepository.getEntryById(request.entryId)
            ?: return prepareFailure(
                BookFailureReason.CONTENT_UNAVAILABLE,
                "The book entry no longer exists.",
                canRetry = false,
            )
        if (visibleEntry.type != EntryType.BOOK) {
            return prepareFailure(BookFailureReason.MALFORMED_CONTENT, "The selected entry is not a book.")
        }
        val chapter = entryChapterRepository.getChapterById(request.chapterId)
            ?: return prepareFailure(
                BookFailureReason.CONTENT_UNAVAILABLE,
                "The selected book item no longer exists.",
                canRetry = false,
            )
        val owner = entryRepository.getEntryById(chapter.entryId)
            ?: return prepareFailure(
                BookFailureReason.CONTENT_UNAVAILABLE,
                "The book item owner no longer exists.",
                canRetry = false,
            )
        if (owner.type != EntryType.BOOK) {
            return prepareFailure(BookFailureReason.MALFORMED_CONTENT, "The selected item does not belong to a book.")
        }
        resolveDownloadedContent(owner, chapter)?.let { downloaded ->
            return preparedSuccess(request, visibleEntry, owner, chapter, downloaded)
        }
        val source = sourceManager.get(owner.source)
            ?: return prepareFailure(
                BookFailureReason.CONTENT_UNAVAILABLE,
                "The book source is not available.",
                canRetry = true,
            )
        val media = try {
            source.getMedia(chapter.toSEntryChapter()) as? EntryMedia.Book
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return prepareFailure(
                BookFailureReason.CONTENT_UNAVAILABLE,
                error.message ?: "The source could not resolve this book item.",
                canRetry = true,
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
        val readerProcessor = readerProcessorRegistry.get(processorId)
            ?: return failure(BookFailureReason.PROCESSOR_UNAVAILABLE, "The selected book reader is unavailable.")
        val visibleEntry = prepared.visibleEntry
        val owner = prepared.owner
        val chapter = prepared.chapter
        val content = prepared.content
        val preparer = when (val selection = preparerRegistry.resolve(content.descriptor)) {
            BookContentPreparerSelection.Unsupported -> {
                return failure(BookFailureReason.FORMAT_UNSUPPORTED, "No preparer supports this book content.")
            }
            is BookContentPreparerSelection.Ambiguous -> {
                return failure(
                    BookFailureReason.PROCESSOR_UNAVAILABLE,
                    "Multiple preparers claim this book content: ${selection.preparers.joinToString { it.id }}",
                )
            }
            is BookContentPreparerSelection.Selected -> selection.preparer
        }
        if (!readerProcessor.supports(preparer.outputModel)) {
            return failure(BookFailureReason.FORMAT_UNSUPPORTED, "The selected reader does not support this model.")
        }

        val progressIdentity = try {
            content.progressIdentity(chapter.id)
        } catch (error: IllegalStateException) {
            return failure(BookFailureReason.MALFORMED_CONTENT, error.message ?: "The book resource is ambiguous.")
        }
        val contentSession = content.createSession(context, owner)
        val preparedPublication = try {
            preparer.prepare(contentSession)
        } catch (error: CancellationException) {
            closeAfterFailure(contentSession, cause = error)
            throw error
        } catch (error: Exception) {
            closeAfterFailure(contentSession, cause = error)
            return failure(
                BookFailureReason.MALFORMED_CONTENT,
                error.message ?: "The book content could not be prepared.",
            )
        }
        return when (preparedPublication) {
            is BookPreparationResult.Failure -> {
                closeAfterFailure(contentSession)
                BookReaderOpenResult.Failure(
                    failure = preparedPublication.failure,
                    canRetry = preparedPublication.canRetry,
                )
            }
            is BookPreparationResult.Success -> {
                val publication = preparedPublication.publication
                try {
                    check(publication.model.descriptor == preparer.outputModel) {
                        "BOOK preparer ${preparer.id} produced ${publication.model.descriptor} instead of " +
                            preparer.outputModel
                    }
                    check(readerProcessor.supports(publication.model.descriptor)) {
                        "The selected reader no longer supports the prepared publication model"
                    }
                    val effectiveProgressIdentity = progressIdentity.copy(
                        resourceRevision = publication.locatorRevision ?: progressIdentity.resourceRevision,
                    )
                    val progress = resolveProgress(
                        chapter = chapter,
                        progressIdentity = effectiveProgressIdentity,
                        preparedPublication = publication,
                    )
                    val decodedLocator = progress
                        ?.takeIf {
                            !chapter.read &&
                                it.hasPartialBookProgress &&
                                (
                                    effectiveProgressIdentity.resourceRevision == null ||
                                        it.resourceRevision == effectiveProgressIdentity.resourceRevision
                                    )
                        }
                        ?.locator
                        ?.let { locator ->
                            BookProgressLocatorCodec.decode(
                                locator = locator,
                                fallbackResourceId = publication.publication.readingOrder.singleOrNull()?.id,
                            )
                        }
                    val initialLocator = publication.restoreLocator(decodedLocator)
                    BookReaderOpenResult.Success(
                        OpenedBookReaderSession(
                            entry = visibleEntry,
                            owner = owner,
                            chapter = chapter,
                            progressIdentity = effectiveProgressIdentity,
                            contentSession = contentSession,
                            preparedPublication = publication,
                            initialLocator = initialLocator,
                            mediaSession = mediaSession,
                            now = now,
                            readerSettingsSurfaceId = readerProcessor.viewerSettingsSurfaceId,
                            readerCapabilities = readerProcessor.readerCapabilities(publication.model),
                        ),
                    )
                } catch (error: CancellationException) {
                    closeAfterFailure(contentSession, publication, error)
                    throw error
                } catch (error: Exception) {
                    closeAfterFailure(contentSession, publication, error)
                    failure(
                        BookFailureReason.CONTENT_UNAVAILABLE,
                        error.message ?: "The saved book position could not be restored.",
                    )
                }
            }
        }
    }

    private suspend fun resolveProgress(
        chapter: EntryChapter,
        progressIdentity: BookProgressIdentity,
        preparedPublication: PreparedBookPublication,
    ): EntryProgressState? {
        var current = entryProgressRepository.get(
            chapter.entryId,
            progressIdentity.contentKey,
            progressIdentity.resourceKey,
        )
        val pendingStates = entryProgressRepository.getByEntryId(chapter.entryId)
            .filter { state ->
                state.chapterId == chapter.id && state.isPendingBookMigration
            }
            .sortedByDescending { maxOf(it.locatorUpdatedAt, it.completionUpdatedAt) }
        pendingStates.forEach { pending ->
            val currentLocator = current
                ?.locator
                ?.let(BookProgressLocatorCodec::decode)
                ?.takeIf(preparedPublication::validate)
            val migratedLocator = if (currentLocator == null) {
                val sourceLocator = BookProgressLocatorCodec.decode(pending.locator)
                try {
                    sourceLocator
                        ?.let { preparedPublication.reconcileMigratedLocator(it) }
                        ?.takeIf(preparedPublication::validate)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    return current
                }
            } else {
                null
            }
            val resolvedPending = pending.copy(
                resourceRevision = progressIdentity.resourceRevision,
                locator = migratedLocator?.let { locator ->
                    BookProgressLocatorCodec.encode(locator, pending.locator.extensions)
                } ?: EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND),
                locatorUpdatedAt = if (migratedLocator == null && current != null) {
                    0L
                } else {
                    pending.locatorUpdatedAt
                },
            )
            entryProgressRepository.upsert(resolvedPending)
            entryProgressRepository.rekey(
                entryId = chapter.entryId,
                chapterId = chapter.id,
                oldContentKey = resolvedPending.contentKey,
                oldResourceKey = resolvedPending.resourceKey,
                newContentKey = progressIdentity.contentKey,
                newResourceKey = progressIdentity.resourceKey,
            )
            current = entryProgressRepository.get(
                chapter.entryId,
                progressIdentity.contentKey,
                progressIdentity.resourceKey,
            )
        }
        return current
    }

    private fun failure(
        reason: BookFailureReason,
        message: String,
        canRetry: Boolean = false,
    ): BookReaderOpenResult.Failure {
        return BookReaderOpenResult.Failure(BookFailure(reason, message), canRetry)
    }

    private fun prepareFailure(
        reason: BookFailureReason,
        message: String,
        canRetry: Boolean = false,
    ): BookReaderPrepareResult.Failure {
        return BookReaderPrepareResult.Failure(BookFailure(reason, message), canRetry)
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
            (downloadCache.getVerified(packageKey) ?: downloadCache.findVerifiedOnDisk(owner, chapter))
                ?.let(PreparedBookContent::Downloaded)
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
        preparedPublication: PreparedBookPublication? = null,
        cause: Throwable? = null,
    ) {
        val closeStack = BookSessionCloseStack().apply {
            own(contentSession)
            preparedPublication?.let(::own)
        }
        runCatching(closeStack::close).exceptionOrNull()?.let { closeError ->
            cause?.addSuppressed(closeError)
        }
    }
}
