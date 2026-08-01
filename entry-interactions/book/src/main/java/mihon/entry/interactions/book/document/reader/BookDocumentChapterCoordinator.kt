package mihon.entry.interactions.book.document.reader

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.book.api.document.locatorAt
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.reader.BookReaderOpenResult
import mihon.entry.interactions.book.reader.BookReaderSessionFactory
import mihon.entry.interactions.book.reader.OpenedBookReaderSession
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPolicy
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.viewer.entryChildWindow
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.EntryChapter

/** Owns bounded adjacent session loading, activation, progress persistence, and completion evidence. */
internal class BookDocumentChapterCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val retainedSessions: BookDocumentReaderSessionViewModel,
    private val sessionFactory: BookReaderSessionFactory,
    private val preparationPreferences: ReaderChapterPreparationPreferences,
    private val currentState: () -> BookDocumentReaderState?,
    private val updateState: (BookDocumentReaderState) -> Unit,
    private val onSessionActivated: (OpenedBookReaderSession) -> Unit,
) : AutoCloseable {
    private val chapterLoadJobs = mutableMapOf<Long, Job>()
    private val completionTracker = BookDocumentCompletionTracker<Long>()
    private var persistLocationJob: Job? = null
    private var readingStartedAt: Long? = null

    fun startReading() {
        if (retainedSessions.currentSession() != null && readingStartedAt == null) {
            readingStartedAt = SystemClock.elapsedRealtime()
        }
    }

    fun stopReading(persist: Boolean) {
        persistLocationJob?.cancel()
        val session = retainedSessions.currentSession()
        val locator = session?.let { retainedSessions.locator(it.chapter.id) }
        val elapsed = consumeReadingDuration()
        if (session != null) {
            scope.launchNonCancellable {
                if (persist) locator?.let { session.saveLocation(it) }
                session.recordHistory(elapsed)
            }
        }
    }

    fun loadChapter(chapter: EntryChapter, activate: Boolean, retry: Boolean) {
        val state = currentState() ?: return
        val adjacent = listOfNotNull(state.window.previous, state.window.next).any { it.id == chapter.id }
        if (!activate && !adjacent) return
        retainedSessions.session(chapter.id)?.let { cached ->
            addLoadedSession(cached, activate)
            return
        }
        if (chapterLoadJobs[chapter.id]?.isActive == true) return
        if (!retry && state.loadStates[chapter.id] is BookDocumentChapterLoadState.Failed) return
        setLoadState(chapter.id, BookDocumentChapterLoadState.Loading)
        chapterLoadJobs[chapter.id] = scope.launch {
            try {
                val current = retainedSessions.currentSession() ?: return@launch
                when (
                    val result = sessionFactory.open(
                        context,
                        BookReaderRequest(current.entry.id, chapter.id),
                        BookDocumentReaderProcessor.PROCESSOR_ID,
                    )
                ) {
                    is BookReaderOpenResult.Failure -> setLoadState(
                        chapter.id,
                        BookDocumentChapterLoadState.Failed(result.failure.message),
                    )
                    is BookReaderOpenResult.Success -> {
                        if (!retainedSessions.cache(result.session)) result.session.close()
                        addLoadedSession(retainedSessions.session(chapter.id) ?: return@launch, activate)
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logcat(LogPriority.ERROR, error) { "Failed to load adjacent BOOK chapter" }
                setLoadState(
                    chapter.id,
                    BookDocumentChapterLoadState.Failed(
                        error.message ?: context.getString(R.string.book_document_chapter_unavailable),
                    ),
                )
            } finally {
                chapterLoadJobs.remove(chapter.id)
            }
        }
    }

    fun onLocation(location: BookDocumentViewerLocation<EntryChapter>) {
        val state = currentState() ?: return
        val chapterId = location.section.owner.id
        if (chapterId != state.currentChapterId) activateChapter(chapterId, completeForwardCrossing = true)
        val session = retainedSessions.session(chapterId) ?: return
        val total = totalBookProgression(state.chapters, chapterId, location.progression)
        val locator = location.section.document.document.locatorAt(location.position).copy(totalProgression = total)
        retainedSessions.updateLocation(chapterId, locator)
        currentState()?.copy(totalProgression = total)?.let(updateState)
        persistLocationJob?.cancel()
        persistLocationJob = scope.launch {
            delay(LOCATION_PERSIST_DEBOUNCE_MILLIS)
            session.saveLocation(locator)
        }
        prepareNextChapterIfNeeded(location.progression.toDouble())
    }

    fun onTerminalObservation(
        chapter: EntryChapter,
        terminalBoundaryVisible: Boolean,
        canScrollForward: Boolean,
        scrollInProgress: Boolean,
    ) {
        if (chapter.id != retainedSessions.currentChapterId) return
        completionTracker.onTerminalObservation(
            chapter.id,
            terminalBoundaryVisible,
            canScrollForward,
            scrollInProgress,
        )?.let(::completeChapter)
    }

    fun prepareNextChapterIfNeeded(progression: Double) {
        val state = currentState() ?: return
        if (ReaderChapterPreparationPolicy.shouldPrepare(
                preparationPreferences.prepareNextChapter.get(),
                progression,
            )
        ) {
            state.window.next?.let { loadChapter(it, activate = false, retry = false) }
        }
    }

    fun prepareCurrentNextChapterIfNeeded() {
        val session = retainedSessions.currentSession() ?: return
        prepareNextChapterIfNeeded(retainedSessions.locator(session.chapter.id)?.progression ?: 0.0)
    }

    override fun close() {
        chapterLoadJobs.values.forEach(Job::cancel)
        chapterLoadJobs.clear()
        persistLocationJob?.cancel()
    }

    private fun addLoadedSession(session: OpenedBookReaderSession, activate: Boolean) {
        val section = session.toDocumentSection(retainedSessions.locator(session.chapter.id)) ?: run {
            setLoadState(
                session.chapter.id,
                BookDocumentChapterLoadState.Failed(context.getString(R.string.book_document_incompatible)),
            )
            return
        }
        val state = currentState() ?: return
        updateState(
            state.copy(
                loadedSections = state.loadedSections + (session.chapter.id to section),
                loadStates = state.loadStates - session.chapter.id,
            ),
        )
        if (activate) activateChapter(session.chapter.id, completeForwardCrossing = false)
    }

    private fun activateChapter(chapterId: Long, completeForwardCrossing: Boolean) {
        val state = currentState() ?: return
        if (chapterId == state.currentChapterId) return
        val previousId = state.currentChapterId
        val previousIndex = state.chapters.indexOfFirst { it.id == previousId }
        val destinationIndex = state.chapters.indexOfFirst { it.id == chapterId }
        val session = retainedSessions.activate(chapterId) ?: return
        val window = state.chapters.entryChildWindow(chapterId, EntryChapter::id) ?: return
        if (completeForwardCrossing && destinationIndex > previousIndex) {
            completionTracker.onForwardChapterActivated(previousId)?.let(::completeChapter)
        }
        recordElapsedFor(previousId)
        val retainedIds = setOfNotNull(window.previous?.id, window.current.id, window.next?.id)
        retainedSessions.retain(retainedIds)
        chapterLoadJobs.keys.toList().filterNot(retainedIds::contains).forEach { id ->
            chapterLoadJobs.remove(id)?.cancel()
        }
        val section = currentState()?.loadedSections?.get(chapterId) ?: return
        val current = currentState() ?: return
        updateState(
            current.copy(
                currentChapterId = chapterId,
                window = window,
                loadedSections = current.loadedSections.filterKeys(retainedIds::contains),
                loadStates = current.loadStates.filterKeys(retainedIds::contains),
                totalProgression = totalBookProgression(
                    state.chapters,
                    chapterId,
                    section.document.document.progressionAt(section.initialPosition),
                ),
                childWebView = null,
            ),
        )
        onSessionActivated(session)
        readingStartedAt = SystemClock.elapsedRealtime()
    }

    private fun completeChapter(chapterId: Long) {
        val session = retainedSessions.session(chapterId) ?: return
        val state = currentState() ?: return
        val section = state.loadedSections[chapterId] ?: return
        val chapterIndex = state.chapters.indexOfFirst { it.id == chapterId }
        val total = ((chapterIndex + 1.0) / state.chapters.size.coerceAtLeast(1)).coerceIn(0.0, 1.0)
        val document = section.document.document
        val locator = document.locatorAt(document.positionAtProgression(1f)).copy(totalProgression = total)
        retainedSessions.updateLocation(chapterId, locator)
        scope.launch { session.saveLocation(locator, completed = true) }
    }

    private fun setLoadState(chapterId: Long, loadState: BookDocumentChapterLoadState) {
        val state = currentState() ?: return
        updateState(state.copy(loadStates = state.loadStates + (chapterId to loadState)))
    }

    private fun recordElapsedFor(chapterId: Long) {
        val elapsed = consumeReadingDuration()
        val session = retainedSessions.session(chapterId) ?: return
        scope.launch { session.recordHistory(elapsed) }
    }

    private fun consumeReadingDuration(): Long {
        val started = readingStartedAt ?: return 0L
        readingStartedAt = null
        return (SystemClock.elapsedRealtime() - started).coerceAtLeast(0L)
    }

    private companion object {
        const val LOCATION_PERSIST_DEBOUNCE_MILLIS = 500L
    }
}
