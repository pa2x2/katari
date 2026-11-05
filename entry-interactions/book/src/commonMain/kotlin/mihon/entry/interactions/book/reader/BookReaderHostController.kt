package mihon.entry.interactions.book.reader

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.book.api.BookFailure
import mihon.book.api.BookFailureReason
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.book.processor.BookReaderProcessor
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.reader.failure.BookReaderChapterHeading
import mihon.entry.interactions.source.EntryChildWebViewResolution
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

/**
 * Opening a book: preparing its chapter, choosing the reader that shows it, asking which one when several can, and
 * the failure when none can. The opened session goes to the chosen reader through [BookReaderSessionRegistry], whose
 * token [onOpened] hands to that reader; a reader that can't be started throws from it.
 */
@Stable
internal class BookReaderHostController(
    private val request: BookReaderRequest,
    private val dependencies: BookReaderDependencies,
    private val context: PlatformContext,
    private val scope: CoroutineScope,
    private val onOpened: (processor: BookReaderProcessor, request: BookReaderRequest, sessionToken: String) -> Unit,
) {
    /** Null while the chapter is prepared or opened. */
    var state by mutableStateOf<BookReaderHostState?>(null)
        private set

    private var launchJob: Job? = null
    private var retrySelection: BookReaderHostState.ReaderSelected? = null

    fun start() = resolve()

    suspend fun loadHeading(): BookReaderChapterHeading? = dependencies.chapterHeadingLoader.load(request)

    fun resolveWebView(sourceChapter: SourceChapter): EntryChildWebViewResolution.Available? =
        dependencies.webViewFeature.resolveChild(sourceChapter.owner, sourceChapter.chapter)
            as? EntryChildWebViewResolution.Available

    fun choose(choice: BookReaderHostState.ChoiceRequired, processorId: String, remember: Boolean) {
        open(dependencies.hostResolver.choose(choice, processorId, remember))
    }

    fun retry() {
        val unavailable = state as? BookReaderHostState.Unavailable ?: return
        if (!unavailable.canRetry) return
        retrySelection?.let(::open) ?: resolve()
    }

    private fun resolve() {
        retrySelection = null
        if (request.entryId < 0L || request.chapterId < 0L) {
            state = BookReaderHostState.Unavailable(
                failure = BookFailure(
                    reason = BookFailureReason.CONTENT_UNAVAILABLE,
                    message = context.stringResource(MR.strings.book_reader_invalid_request),
                ),
            )
            return
        }
        runStartup(retrySelection = null) {
            when (val resolved = dependencies.hostResolver.resolve(request.entryId, request.chapterId)) {
                is BookReaderHostState.ReaderSelected -> openSelected(resolved)
                else -> state = resolved
            }
        }
    }

    private fun open(selected: BookReaderHostState.ReaderSelected) {
        retrySelection = null
        runStartup(retrySelection = selected) { openSelected(selected) }
    }

    private suspend fun openSelected(selected: BookReaderHostState.ReaderSelected) {
        val result = dependencies.sessionFactory.openPrepared(
            prepared = selected.prepared,
            processorId = selected.processor.id,
        )
        when (result) {
            is BookReaderOpenResult.Failure -> {
                retrySelection = selected.takeIf { result.canRetry }
                state = BookReaderHostState.Unavailable(
                    failure = result.failure,
                    descriptor = selected.descriptor,
                    canRetry = result.canRetry,
                )
            }
            is BookReaderOpenResult.Success -> {
                val registry = dependencies.sessionRegistry
                val token = registry.register(result.session)
                try {
                    onOpened(selected.processor, selected.prepared.request, token)
                } catch (error: Exception) {
                    registry.discard(token)
                    retrySelection = null
                    state = BookReaderHostState.Unavailable(
                        failure = BookFailure(
                            reason = BookFailureReason.PROCESSOR_UNAVAILABLE,
                            message = error.message ?: "The selected book reader could not be started.",
                        ),
                        descriptor = selected.descriptor,
                    )
                }
            }
        }
    }

    private fun runStartup(retrySelection: BookReaderHostState.ReaderSelected?, block: suspend () -> Unit) {
        launchJob?.cancel()
        state = null
        launchJob = scope.launch {
            try {
                block()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logcat(LogPriority.ERROR, error) { "Unexpected BOOK reader startup failure" }
                this@BookReaderHostController.retrySelection = retrySelection
                state = BookReaderHostState.Unavailable(
                    failure = BookFailure(
                        reason = BookFailureReason.CONTENT_UNAVAILABLE,
                        message = error.message ?: "The book reader could not be opened.",
                    ),
                    descriptor = retrySelection?.descriptor,
                    canRetry = true,
                )
            }
        }
    }
}
