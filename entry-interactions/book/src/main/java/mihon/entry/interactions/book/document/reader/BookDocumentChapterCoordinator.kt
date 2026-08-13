package mihon.entry.interactions.book.document.reader

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.book.api.document.locatorAt
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.reader.BookReaderOpenResult
import mihon.entry.interactions.book.reader.BookReaderSessionFactory
import mihon.entry.interactions.book.reader.OpenedBookReaderSession
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPolicy
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.EntryChapter

/** Owns bounded adjacent session loading, activation, progress persistence, and completion evidence. */
internal class BookDocumentChapterCoordinator(
    private val context: Context,
    private val scope: CoroutineScope,
    private val retainedSessions: BookDocumentReaderSessionViewModel,
    private val sessionFactory: BookReaderSessionFactory,
    private val isNextChapterPreparationEnabled: () -> Boolean,
    private val currentState: () -> BookDocumentReaderState?,
    private val updateState: (BookDocumentReaderState) -> Unit,
    private val onSessionActivated: (OpenedBookReaderSession) -> Unit,
) : AutoCloseable {
    private val chapterLoadJobs = mutableMapOf<Long, Job>()
    private val completionTracker = BookDocumentCompletionTracker<Long>()
    private val chapterSelectionRequests = mutableSetOf<Long>()
    private var persistLocationJob: Job? = null
    private var readingStartedAt: Long? = null
    private var navigationRequestId = 0L

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
        if (!activate && chapter.id in state.loadedSections) return
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
                    is BookReaderOpenResult.Failure -> {
                        chapterSelectionRequests.remove(chapter.id)
                        setLoadState(
                            chapter.id,
                            BookDocumentChapterLoadState.Failed(result.failure.message),
                        )
                    }
                    is BookReaderOpenResult.Success -> {
                        if (!retainedSessions.cache(result.session)) result.session.close()
                        addLoadedSession(
                            retainedSessions.session(chapter.id) ?: return@launch,
                            activate || chapter.id in chapterSelectionRequests,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                chapterSelectionRequests.remove(chapter.id)
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

    fun selectChapter(chapter: EntryChapter, retry: Boolean) {
        chapterSelectionRequests += chapter.id
        loadChapter(chapter, activate = true, retry = retry)
    }

    fun onLocation(location: BookDocumentViewerLocation<EntryChapter>) {
        val state = currentState() ?: return
        val chapterId = location.section.owner.id
        val navigationRequest = state.navigationRequest
        if (!navigationRequest.acceptsLocation(chapterId, location.position)) return
        val chapterActivated =
            chapterId != state.currentChapterId &&
                activateChapter(
                    chapterId = chapterId,
                    completeForwardCrossing = true,
                    observedLocation = location,
                    observedNavigationRequest = navigationRequest,
                )
        val session = retainedSessions.session(chapterId) ?: return
        val total = state.readingOrder.totalProgression(chapterId, location.progression)
        val locator = location.section.document.document.locatorAt(location.position).copy(totalProgression = total)
        retainedSessions.updateLocation(chapterId, locator)
        if (!chapterActivated) {
            currentState()?.let { current ->
                updateState(
                    current.copy(
                        visualChapterProgression = location.visualProgression,
                        navigationRequest = current.navigationRequest.afterAcceptedLocation(
                            observedRequest = navigationRequest,
                            chapterId = chapterId,
                        ),
                    ),
                )
            }
        }
        persistLocationJob?.cancel()
        persistLocationJob = scope.launchIO {
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
                isNextChapterPreparationEnabled(),
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
        chapterSelectionRequests.clear()
        persistLocationJob?.cancel()
    }

    private fun addLoadedSession(session: OpenedBookReaderSession, activate: Boolean) {
        val explicitSelection = chapterSelectionRequests.remove(session.chapter.id)
        val restoredSection = session.toDocumentSection(retainedSessions.locator(session.chapter.id)) ?: run {
            setLoadState(
                session.chapter.id,
                BookDocumentChapterLoadState.Failed(context.getString(R.string.book_document_incompatible)),
            )
            return
        }
        val section = if (explicitSelection) {
            restoredSection.fromBeginningForExplicitNavigation()
        } else {
            restoredSection
        }
        val state = currentState() ?: return
        updateState(
            state.copy(
                loadedSections = state.loadedSections + (session.chapter.id to section),
                loadStates = state.loadStates - session.chapter.id,
            ),
        )
        if (activate) activateChapter(session.chapter.id, completeForwardCrossing = false)
        if (explicitSelection) requestNavigation(section)
    }

    private fun requestNavigation(section: BookDocumentSection<EntryChapter>) {
        val state = currentState() ?: return
        val position = section.initialPosition
        val progression = section.document.document.progressionAt(position)
        val total = state.readingOrder.totalProgression(section.owner.id, progression)
        retainedSessions.updateLocation(
            section.owner.id,
            section.document.document.locatorAt(position).copy(totalProgression = total),
        )
        navigationRequestId += 1
        updateState(
            state.copy(
                visualChapterProgression = progression,
                navigationRequest = BookDocumentNavigationRequest(
                    id = navigationRequestId,
                    chapterId = section.owner.id,
                    position = position,
                ),
            ),
        )
    }

    private fun activateChapter(
        chapterId: Long,
        completeForwardCrossing: Boolean,
        observedLocation: BookDocumentViewerLocation<EntryChapter>? = null,
        observedNavigationRequest: BookDocumentNavigationRequest? = null,
    ): Boolean {
        val state = currentState() ?: return false
        if (chapterId == state.currentChapterId) return false
        val previousId = state.currentChapterId
        val previousIndex = state.readingOrder.indexOf(previousId)
        val destinationIndex = state.readingOrder.indexOf(chapterId)
        val session = retainedSessions.activate(chapterId) ?: return false
        val window = state.readingOrder.window(chapterId) ?: return false
        if (completeForwardCrossing && destinationIndex > previousIndex) {
            completionTracker.onForwardChapterActivated(previousId)?.let(::completeChapter)
        }
        recordElapsedFor(previousId)
        val retainedIds = setOfNotNull(window.previous?.id, window.current.id, window.next?.id)
        retainedSessions.retain(retainedIds)
        chapterLoadJobs.keys.toList().filterNot(retainedIds::contains).forEach { id ->
            chapterLoadJobs.remove(id)?.cancel()
        }
        chapterSelectionRequests.retainAll(retainedIds)
        val section = currentState()?.loadedSections?.get(chapterId) ?: return false
        val current = currentState() ?: return false
        val progression = observedLocation?.progression
            ?: section.document.document.progressionAt(section.initialPosition)
        updateState(
            current.copy(
                currentChapterId = chapterId,
                window = window,
                loadedSections = current.loadedSections.filterKeys(retainedIds::contains),
                loadStates = current.loadStates.filterKeys(retainedIds::contains),
                visualChapterProgression = observedLocation?.visualProgression ?: progression,
                childWebView = null,
                navigationRequest = current.navigationRequest.afterAcceptedLocation(
                    observedRequest = observedNavigationRequest,
                    chapterId = chapterId,
                ),
            ),
        )
        onSessionActivated(session)
        readingStartedAt = SystemClock.elapsedRealtime()
        return true
    }

    private fun completeChapter(chapterId: Long) {
        val session = retainedSessions.session(chapterId) ?: return
        val state = currentState() ?: return
        val section = state.loadedSections[chapterId] ?: return
        val total = state.readingOrder.completedProgression(chapterId)
        val document = section.document.document
        scope.launchNonCancellable {
            val locator = document.locatorAt(document.positionAtProgression(1f)).copy(totalProgression = total)
            withContext(Dispatchers.Main.immediate) {
                retainedSessions.updateLocation(chapterId, locator)
            }
            session.saveLocation(locator, completed = true)
        }
    }

    private fun setLoadState(chapterId: Long, loadState: BookDocumentChapterLoadState) {
        val state = currentState() ?: return
        updateState(state.copy(loadStates = state.loadStates + (chapterId to loadState)))
    }

    private fun recordElapsedFor(chapterId: Long) {
        val elapsed = consumeReadingDuration()
        val session = retainedSessions.session(chapterId) ?: return
        scope.launchNonCancellable { session.recordHistory(elapsed) }
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
