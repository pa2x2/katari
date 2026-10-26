package mihon.entry.interactions.book.document.reader

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import mihon.book.api.BookLocator
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.locatorAt
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import mihon.entry.interactions.book.document.reader.navigation.BookDocumentNavigationTarget
import mihon.entry.interactions.book.document.reader.navigation.resolveNavigationDestination
import mihon.entry.interactions.book.document.render.toPreparedBookDocument
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.reader.BookReaderOpenResult
import mihon.entry.interactions.book.reader.BookReaderSessionFactory
import mihon.entry.interactions.book.reader.OpenedBookReaderSession
import mihon.entry.interactions.media.session.EntryMediaSessionActivitySession
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
    private val updateVisualChapterProgression: (Float) -> Unit,
    private val onNavigationMissing: () -> Unit,
    private val onSessionActivated: (OpenedBookReaderSession) -> Unit,
) : AutoCloseable {
    private val chapterLoadJobs = mutableMapOf<Long, Job>()
    private val completionTracker = BookDocumentCompletionTracker<Long>()
    private val chapterSelectionRequests = mutableSetOf<Long>()
    private var persistLocationJob: Job? = null
    private var activityCheckpointJob: Job? = null
    private var readingStartedAt: Long? = null
    private val activitySession = EntryMediaSessionActivitySession()
    private val activityMutex = Mutex()
    private var navigationRequestId = 0L
    private var pendingNavigationTarget: BookDocumentNavigationTarget? = null

    /** A selected chapter whose opening records a return point; nothing moves until it opens. */
    private var originRecordingChapterId: Long? = null

    fun startReading() {
        if (retainedSessions.currentSession() != null && readingStartedAt == null) {
            readingStartedAt = SystemClock.elapsedRealtime()
        }
        if (retainedSessions.currentSession() != null && activityCheckpointJob?.isActive != true) {
            activityCheckpointJob = scope.launchIO {
                while (isActive) {
                    delay(ACTIVITY_CHECKPOINT_INTERVAL_MILLIS)
                    checkpointReading()
                }
            }
        }
    }

    fun stopReading(persist: Boolean) {
        activityCheckpointJob?.cancel()
        activityCheckpointJob = null
        persistLocationJob?.cancel()
        val session = retainedSessions.currentSession()
        val locator = session?.let { retainedSessions.locator(it.chapter.id) }
        val elapsed = consumeReadingDuration()
        if (session != null) {
            scope.launchNonCancellable {
                if (persist) locator?.let { session.saveLocation(it) }
                recordActivity(session, elapsed)
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
        setLoadState(chapter, BookDocumentChapterLoadState.Loading)
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
                        chapter,
                        BookDocumentChapterLoadState.Failed(result.failure.message),
                        selected = chapterSelectionRequests.remove(chapter.id),
                    )
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
                logcat(LogPriority.ERROR, error) { "Failed to load adjacent BOOK chapter" }
                setLoadState(
                    chapter,
                    BookDocumentChapterLoadState.Failed(
                        error.message ?: context.getString(R.string.book_document_chapter_unavailable),
                    ),
                    selected = chapterSelectionRequests.remove(chapter.id),
                )
            } finally {
                chapterLoadJobs.remove(chapter.id)
            }
        }
    }

    fun selectChapter(chapter: EntryChapter, retry: Boolean, rememberOrigin: Boolean = false) {
        dismissChapterSelection()
        chapterSelectionRequests += chapter.id
        originRecordingChapterId = chapter.id.takeIf { rememberOrigin }
        loadChapter(chapter, activate = true, retry = retry)
    }

    fun navigateLink(source: BookDocumentSection<EntryChapter>, target: BookDocumentLinkTarget) {
        val locator = when (target) {
            is BookDocumentLinkTarget.Anchor -> BookLocator(
                source.document.document.resourceId,
                fragments = listOf(target.fragment),
            )
            is BookDocumentLinkTarget.Resource -> BookLocator(
                target.resourceId,
                fragments = listOfNotNull(target.fragment),
            )
            is BookDocumentLinkTarget.Reference -> BookLocator(
                target.resourceId ?: source.document.document.resourceId,
                fragments = listOf(target.fragment),
            )
            is BookDocumentLinkTarget.External -> return
        }
        navigateWithinPublication(source.owner.id, locator, target is BookDocumentLinkTarget.Reference)
    }

    /** [rememberOrigin] false moves without leaving a return point, as closing a search goes back to where it began. */
    fun selectNavigationTarget(
        target: BookDocumentNavigationTarget,
        rememberOrigin: Boolean = !target.returnToOrigin,
    ) {
        dismissChapterSelection()
        pendingNavigationTarget = null
        val locator = target.locator
        if (locator != null && retainedSessions.session(target.chapter.id) != null) {
            navigateWithinPublication(
                target.chapter.id,
                locator,
                restorePosition = target.restorePosition,
                returnToOrigin = target.returnToOrigin,
                rememberOrigin = rememberOrigin,
            )
            return
        }
        if (locator != null) pendingNavigationTarget = target
        selectChapter(target.chapter, retry = true, rememberOrigin = rememberOrigin)
    }

    /** Shows a search match of the open chapter in the middle of the page, leaving no return point. */
    fun revealSearchMatch(sectionKey: String, offset: Int) {
        val state = currentState() ?: return
        val section = state.loadedSections[state.currentChapterId]?.sections?.firstOrNull { it.key == sectionKey }
            ?: return
        requestNavigation(section, section.document.document.positionAtLogicalOffset(offset), centerLine = true)
    }

    private fun navigateWithinPublication(
        chapterId: Long,
        locator: BookLocator,
        contextual: Boolean = false,
        restorePosition: Boolean = false,
        returnToOrigin: Boolean = false,
        rememberOrigin: Boolean = true,
    ) {
        val session = retainedSessions.session(chapterId)
        val publication = session?.preparedPublication as? PreparedBookDocumentPublication
        val destination = publication?.resolveNavigationDestination(locator, contextual, restorePosition)
        if (session == null || destination == null) {
            onNavigationMissing()
            return
        }
        val state = currentState() ?: return
        if (destination.contextual) {
            updateState(
                state.copy(
                    auxiliarySection = BookDocumentSection(
                        key = "$chapterId:reference:${destination.document.resourceId}:${destination.position}",
                        owner = session.chapter,
                        document = destination.document.toPreparedBookDocument(),
                        initialPosition = destination.position,
                        resourceLoader = publication.resourceLoader,
                    ),
                ),
            )
        } else {
            val section = state.loadedSections[chapterId]?.sections?.firstOrNull {
                it.document.document.resourceId == locator.resourceId
            } ?: run {
                onNavigationMissing()
                return
            }
            if (rememberOrigin) retainedSessions.jumpHistory.rememberOrigin(section.key, destination.position)
            if (chapterId != state.currentChapterId) activateChapter(chapterId, completeForwardCrossing = false)
            dismissAuxiliarySection()
            requestNavigation(
                section,
                destination.position,
                alignToPassage = restorePosition,
                returnToOrigin = returnToOrigin,
            )
        }
    }

    fun retryChapterSelection() {
        val selection = currentState()?.chapterSelection ?: return
        chapterSelectionRequests += selection.chapter.id
        loadChapter(selection.chapter, activate = true, retry = true)
    }

    /** Drops the feedback of a pending chapter selection; the reader stays where it is. */
    fun dismissChapterSelection() {
        val state = currentState() ?: return
        val selection = state.chapterSelection ?: return
        if (pendingNavigationTarget?.chapter?.id == selection.chapter.id) pendingNavigationTarget = null
        originRecordingChapterId = null
        updateState(state.copy(chapterSelection = null))
    }

    fun dismissAuxiliarySection() {
        val state = currentState() ?: return
        if (state.auxiliarySection != null) updateState(state.copy(auxiliarySection = null))
    }

    fun onLocation(location: BookDocumentViewerLocation<EntryChapter>) {
        val state = currentState() ?: return
        val chapterId = location.section.owner.id
        val navigationRequest = state.navigationRequest
        if (!navigationRequest.acceptsLocation(
                chapterId,
                location.position,
                location.section.key,
                location.restoredNavigationId,
            )
        ) {
            return
        }
        if (navigationRequest?.returnToOrigin == true) retainedSessions.jumpHistory.dismiss()
        val chapterActivated =
            chapterId != state.currentChapterId &&
                activateChapter(
                    chapterId = chapterId,
                    completeForwardCrossing = true,
                    observedLocation = location,
                    observedNavigationRequest = navigationRequest,
                )
        val session = retainedSessions.session(chapterId) ?: return
        val publicationProgression = location.section.totalProgression(location.progression)
        val total = publicationProgression.toDouble()
        val locator = location.section.document.document.locatorAt(location.position).copy(totalProgression = total)
        retainedSessions.updateLocation(chapterId, locator)
        if (!chapterActivated) {
            updateVisualChapterProgression(location.section.totalProgression(location.visualProgression))
            currentState()?.let { current ->
                val acceptedNavigationRequest = current.navigationRequest.afterAcceptedLocation(
                    observedRequest = navigationRequest,
                    chapterId = chapterId,
                )
                if (acceptedNavigationRequest != current.navigationRequest) {
                    updateState(current.copy(navigationRequest = acceptedNavigationRequest))
                }
            }
        }
        persistLocationJob?.cancel()
        persistLocationJob = scope.launchIO {
            delay(LOCATION_PERSIST_DEBOUNCE_MILLIS)
            session.saveLocation(locator)
        }
        prepareNextChapterIfNeeded(publicationProgression.toDouble())
    }

    fun onUserScrollStarted() {
        val state = currentState() ?: return
        val navigationRequest = state.navigationRequest.afterUserScrollStarted()
        if (navigationRequest == state.navigationRequest) return
        updateState(state.copy(navigationRequest = navigationRequest))
    }

    /**
     * Consumes content-derived chapter-end observations from the viewers. Completion evidence
     * reaches here through reading-mode layouts (settled end of the current chapter's content),
     * never through the chapter-transition row's visibility.
     */
    fun onChapterEndObservation(chapter: EntryChapter, chapterEndVisible: Boolean, scrollInProgress: Boolean) {
        if (chapter.id != retainedSessions.currentChapterId) return
        completionTracker.onChapterEndObservation(chapter.id, chapterEndVisible, scrollInProgress)
            ?.let(::completeChapter)
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
        prepareNextChapterIfNeeded(
            retainedSessions.locator(session.chapter.id)?.let(session.preparedPublication::progression) ?: 0.0,
        )
    }

    override fun close() {
        chapterLoadJobs.values.forEach(Job::cancel)
        chapterLoadJobs.clear()
        chapterSelectionRequests.clear()
        pendingNavigationTarget = null
        originRecordingChapterId = null
        persistLocationJob?.cancel()
        activityCheckpointJob?.cancel()
        activityCheckpointJob = null
    }

    private fun addLoadedSession(session: OpenedBookReaderSession, activate: Boolean) {
        val explicitSelection = chapterSelectionRequests.remove(session.chapter.id)
        val restoredSections = session.toDocumentSections(retainedSessions.locator(session.chapter.id)) ?: run {
            setLoadState(
                session.chapter,
                BookDocumentChapterLoadState.Failed(context.getString(R.string.book_document_incompatible)),
                selected = explicitSelection,
            )
            return
        }
        val sections = if (explicitSelection) {
            restoredSections.fromBeginningForExplicitNavigation()
        } else {
            restoredSections
        }
        val state = currentState() ?: return
        updateState(
            state.copy(
                loadedSections = state.loadedSections + (session.chapter.id to sections),
                loadStates = state.loadStates - session.chapter.id,
                publicationNavigation = state.publicationNavigation +
                    (session.chapter.id to session.preparedPublication.publication.navigation),
            ),
        )
        if (explicitSelection && originRecordingChapterId == session.chapter.id) {
            originRecordingChapterId = null
            retainedSessions.jumpHistory.rememberOrigin()
        }
        if (activate) activateChapter(session.chapter.id, completeForwardCrossing = false)
        val pending = pendingNavigationTarget?.takeIf { it.chapter.id == session.chapter.id }
        if (pending != null) {
            pendingNavigationTarget = null
            navigateWithinPublication(
                session.chapter.id,
                checkNotNull(pending.locator),
                restorePosition = pending.restorePosition,
                returnToOrigin = pending.returnToOrigin,
                rememberOrigin = false,
            )
        } else if (explicitSelection) {
            requestNavigation(sections.initialSection)
        }
    }

    private fun requestNavigation(
        section: BookDocumentSection<EntryChapter>,
        position: mihon.book.api.document.BookDocumentPosition = section.initialPosition,
        alignToPassage: Boolean = false,
        returnToOrigin: Boolean = false,
        centerLine: Boolean = false,
    ) {
        val state = currentState() ?: return
        val progression = section.document.document.progressionAt(position)
        val publicationProgression = section.totalProgression(progression)
        val total = publicationProgression.toDouble()
        retainedSessions.updateLocation(
            section.owner.id,
            section.document.document.locatorAt(position).copy(totalProgression = total),
        )
        navigationRequestId += 1
        updateVisualChapterProgression(publicationProgression)
        updateState(
            state.copy(
                navigationRequest = BookDocumentNavigationRequest(
                    id = navigationRequestId,
                    chapterId = section.owner.id,
                    sectionKey = section.key,
                    position = position,
                    alignToPassage = alignToPassage,
                    returnToOrigin = returnToOrigin,
                    centerLine = centerLine,
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
        val section = observedLocation?.section
            ?: currentState()?.loadedSections?.get(chapterId)?.initialSection
            ?: return false
        val current = currentState() ?: return false
        val progression = observedLocation?.let { section.totalProgression(it.progression) }
            ?: section.totalProgression(section.document.document.progressionAt(section.initialPosition))
        updateState(
            current.copy(
                currentChapterId = chapterId,
                window = window,
                loadedSections = current.loadedSections.filterKeys(retainedIds::contains),
                loadStates = current.loadStates.filterKeys(retainedIds::contains),
                // Leaving the window cancels a selected chapter's load, so its feedback goes too.
                chapterSelection = current.chapterSelection?.takeIf {
                    it.chapter.id != chapterId && it.chapter.id in retainedIds
                },
                childWebView = null,
                navigationRequest = current.navigationRequest.afterAcceptedLocation(
                    observedRequest = observedNavigationRequest,
                    chapterId = chapterId,
                ),
            ),
        )
        updateVisualChapterProgression(
            observedLocation?.let { section.totalProgression(it.visualProgression) } ?: progression,
        )
        onSessionActivated(session)
        readingStartedAt = SystemClock.elapsedRealtime()
        return true
    }

    private fun completeChapter(chapterId: Long) {
        val session = retainedSessions.session(chapterId) ?: return
        val state = currentState() ?: return
        val section = state.loadedSections[chapterId]?.sections?.lastOrNull() ?: return
        val total = 1.0
        val document = section.document.document
        scope.launchNonCancellable {
            val locator = document.locatorAt(document.positionAtProgression(1f)).copy(totalProgression = total)
            withContext(Dispatchers.Main.immediate) {
                retainedSessions.updateLocation(chapterId, locator)
            }
            session.saveLocation(locator, completed = true)
        }
    }

    private fun setLoadState(
        chapter: EntryChapter,
        loadState: BookDocumentChapterLoadState,
        selected: Boolean = chapter.id in chapterSelectionRequests,
    ) {
        val state = currentState() ?: return
        updateState(
            state.copy(
                loadStates = state.loadStates + (chapter.id to loadState),
                chapterSelection = if (selected) {
                    BookDocumentChapterSelectionLoad(chapter, loadState)
                } else {
                    state.chapterSelection
                },
            ),
        )
    }

    private fun recordElapsedFor(chapterId: Long) {
        val elapsed = consumeReadingDuration()
        val session = retainedSessions.session(chapterId) ?: return
        scope.launchNonCancellable { recordActivity(session, elapsed) }
    }

    private suspend fun recordActivity(session: OpenedBookReaderSession, durationMillis: Long) {
        activityMutex.withLock {
            withContext(NonCancellable) {
                session.recordHistory(durationMillis, activitySession)
            }
        }
    }

    private suspend fun checkpointReading() {
        val session = retainedSessions.currentSession() ?: return
        val elapsed = consumeReadingDuration()
        readingStartedAt = SystemClock.elapsedRealtime()
        recordActivity(session, elapsed)
    }

    private fun consumeReadingDuration(): Long {
        val started = readingStartedAt ?: return 0L
        readingStartedAt = null
        return (SystemClock.elapsedRealtime() - started).coerceAtLeast(0L)
    }

    private companion object {
        const val LOCATION_PERSIST_DEBOUNCE_MILLIS = 500L
        const val ACTIVITY_CHECKPOINT_INTERVAL_MILLIS = 30_000L
    }
}
