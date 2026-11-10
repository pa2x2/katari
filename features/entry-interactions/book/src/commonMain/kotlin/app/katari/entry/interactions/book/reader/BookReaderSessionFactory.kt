package app.katari.entry.interactions.book.reader

import app.katari.book.api.BookFailure
import app.katari.book.api.BookFailureReason
import app.katari.entry.interactions.book.content.AppBookExternalResourceResolver
import app.katari.entry.interactions.book.content.BookContentSession
import app.katari.entry.interactions.book.content.BookMaterializationStore
import app.katari.entry.interactions.book.content.SourceBookContentSession
import app.katari.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import app.katari.entry.interactions.book.document.rules.BookTextRuleSet
import app.katari.entry.interactions.book.download.BookDownloadCache
import app.katari.entry.interactions.book.download.BookDownloadPackageKey
import app.katari.entry.interactions.book.download.DownloadedBookContentSession
import app.katari.entry.interactions.book.preparation.BookContentPreparerRegistry
import app.katari.entry.interactions.book.preparation.BookContentPreparerSelection
import app.katari.entry.interactions.book.preparation.BookOpeningProgress
import app.katari.entry.interactions.book.preparation.BookPreparationResult
import app.katari.entry.interactions.book.preparation.PreparedBookPublication
import app.katari.entry.interactions.book.processor.BookReaderProcessorRegistry
import app.katari.entry.interactions.book.processor.BookReaderRequest
import app.katari.entry.interactions.book.state.BookProgressIdentity
import app.katari.entry.interactions.book.state.BookProgressLocatorCodec
import app.katari.entry.interactions.book.state.hasPartialBookProgress
import app.katari.entry.interactions.media.EntryMediaSessionProcessor
import app.katari.entry.interactions.reader.settings.BookTextRule
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.ArtworkSource
import eu.kanade.tachiyomi.source.entry.EntryMedia
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.storage.StorageFileResolver
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.adapter.toSEntryChapter
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
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
    private val files: StorageFileResolver,
    private val materializationStore: BookMaterializationStore,
    private val downloadCache: BookDownloadCache,
    private val mediaSession: EntryMediaSessionProcessor,
    private val textRules: () -> List<BookTextRule> = { emptyList() },
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun open(
        request: BookReaderRequest,
        processorId: String,
        onProgress: (BookOpeningProgress) -> Unit = {},
    ): BookReaderOpenResult {
        return when (val prepared = prepare(request, onProgress = onProgress)) {
            is BookReaderPrepareResult.Failure -> BookReaderOpenResult.Failure(
                failure = prepared.failure,
                canRetry = prepared.canRetry,
                sourceChapter = prepared.sourceChapter,
            )
            is BookReaderPrepareResult.Success -> openPrepared(prepared.request, processorId, onProgress)
        }
    }

    /**
     * Opens the chapter only from its download, never asking the source, as searching a book's downloaded chapters
     * does; null when it isn't downloaded or can't be opened.
     */
    suspend fun openDownloaded(
        request: BookReaderRequest,
        processorId: String,
    ): OpenedBookReaderSession? = when (val prepared = prepare(request, downloadedOnly = true)) {
        is BookReaderPrepareResult.Failure -> null
        is BookReaderPrepareResult.Success ->
            (openPrepared(prepared.request, processorId) as? BookReaderOpenResult.Success)?.session
    }

    suspend fun prepare(
        request: BookReaderRequest,
        downloadedOnly: Boolean = false,
        onProgress: (BookOpeningProgress) -> Unit = {},
    ): BookReaderPrepareResult {
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
        if (downloadedOnly) {
            return prepareFailure(BookFailureReason.CONTENT_UNAVAILABLE, "The book item is not downloaded.")
        }
        val source = sourceManager.get(owner.source)
            ?: return prepareFailure(
                BookFailureReason.CONTENT_UNAVAILABLE,
                "The book source is not available.",
                canRetry = true,
            )
        onProgress(BookOpeningProgress.Requesting)
        val media = try {
            // Sources read responses on the thread they resume on, and the reader prepares from the main thread.
            withContext(Dispatchers.IO) { source.getMedia(chapter.toSEntryChapter()) } as? EntryMedia.Book
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            return BookReaderPrepareResult.Failure(
                BookFailure(
                    BookFailureReason.CONTENT_UNAVAILABLE,
                    error.message ?: "The source could not resolve this book item.",
                ),
                canRetry = true,
                sourceChapter = SourceChapter(owner, chapter, source.name),
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
        prepared: PreparedBookReaderRequest,
        processorId: String,
        onProgress: (BookOpeningProgress) -> Unit = {},
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
        val contentSession = content.createSession(owner)
        val preparedPublication = try {
            preparer.prepare(contentSession, onProgress)
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
                val publication = preparedPublication.publication.withTextRules(owner)
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
                    val progress = BookReaderProgressResolver(entryProgressRepository).resolve(
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
                            sourceName = sourceManager.get(owner.source)?.name,
                            downloaded = contentSession is DownloadedBookContentSession,
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

    /** The reader's text rules for [owner] applied to a document publication; a rule that breaks one is skipped. */
    private fun PreparedBookPublication.withTextRules(owner: Entry): PreparedBookPublication {
        val documents = this as? PreparedBookDocumentPublication ?: return this
        val rules = BookTextRuleSet.forSeries(textRules(), owner.source, owner.url) ?: return this
        return try {
            documents.withDocuments(rules::apply)
        } catch (error: IllegalArgumentException) {
            logcat(LogPriority.WARN, error) { "BOOK text rules could not be applied to a chapter" }
            this
        }
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

    private fun PreparedBookContent.createSession(owner: Entry): BookContentSession = when (this) {
        is PreparedBookContent.Downloaded -> DownloadedBookContentSession(download, materializationStore)
        is PreparedBookContent.Source -> SourceBookContentSession(
            source = source,
            entry = owner,
            media = media,
            externalResolver = AppBookExternalResourceResolver(
                files = files,
                httpClient = (source as? ArtworkSource)?.client ?: networkHelper.client,
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
