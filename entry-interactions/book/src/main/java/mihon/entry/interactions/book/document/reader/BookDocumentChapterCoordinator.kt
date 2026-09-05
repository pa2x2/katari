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
import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
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

    fun navigateLink(
        source: BookDocumentSection<EntryChapter>,
        target: BookDocumentLinkTarget,
    ) {
        val resourceId: String
        val fragment: String?
        when (target) {
            is BookDocumentLinkTarget.Anchor -> {
                resourceId = source.document.document.resourceId
                fragment = target.fragment
            }
            is BookDocumentLinkTarget.Resource -> {
                resourceId = target.resourceId
                fragment = target.fragment
            }
            is BookDocumentLinkTarget.Reference -> {
                openContextualReference(source, target)
                return
            }
            is BookDocumentLinkTarget.External -> return
        }
        navigateWithinPublication(
            BookLocator(resourceId = resourceId, fragments = listOfNotNull(fragment)),
        )
    }

    private fun openContextualReference(
        sourceSection: BookDocumentSection<EntryChapter>,
        target: BookDocumentLinkTarget.Reference,
    ) {
        val state = currentState() ?: return
        val session = retainedSessions.currentSession() ?: return
        val publication = session.preparedPublication as? PreparedBookDocumentPublication ?: return
        val source = publication.document(target.resourceId ?: sourceSection.document.document.resourceId) ?: return
        val position = source.anchors[target.fragment] ?: return
        updateState(
            state.copy(
                auxiliarySection = BookDocumentSection(
                    key = "${state.currentChapterId}:reference:${source.resourceId}:${target.fragment}",
                    owner = session.chapter,
                    document = source.toPreparedBookDocument(),
                    initialPosition = position,
                    resourceLoader = publication.resourceLoader,
                ),
            ),
        )
    }

    fun navigateWithinPublication(locator: BookLocator) {
        val state = currentState() ?: return
        val sections = state.loadedSections[state.currentChapterId] ?: return
        val section = sections.sections.firstOrNull {
            it.document.document.resourceId == locator.resourceId
        } ?: auxiliarySection(locator, state) ?: return
        val position = locator.fragments.firstNotNullOfOrNull { fragment ->
            section.document.document.anchors[fragment]
        } ?: section.document.document.resolvePosition(locator)
            ?: section.document.document.positionAtProgression(0f)
        if (section !in sections.sections) {
            updateState(state.copy(auxiliarySection = section))
            return
        }
        if (state.auxiliarySection != null) updateState(state.copy(auxiliarySection = null))
        requestNavigation(section, position)
    }

    fun dismissAuxiliarySection() {
        val state = currentState() ?: return
        if (state.auxiliarySection != null) updateState(state.copy(auxiliarySection = null))
    }

    private fun auxiliarySection(
        locator: BookLocator,
        state: BookDocumentReaderState,
    ): BookDocumentSection<EntryChapter>? {
        val session = retainedSessions.currentSession() ?: return null
        val publication = session.preparedPublication as? PreparedBookDocumentPublication ?: return null
        val source = publication.document(locator.resourceId) ?: return null
        if (source.resourceId in publication.publication.readingOrder.map { it.id }) return null
        val prepared = source.toPreparedBookDocument()
        val position = locator.fragments.firstNotNullOfOrNull(source.anchors::get)
            ?: source.resolvePosition(locator)
            ?: source.positionAtProgression(0f)
        return BookDocumentSection(
            key = "${state.currentChapterId}:auxiliary:${source.resourceId}",
            owner = session.chapter,
            document = prepared,
            initialPosition = position,
            resourceLoader = publication.resourceLoader,
        )
    }

    fun onLocation(location: BookDocumentViewerLocation<EntryChapter>) {
        val state = currentState() ?: return
        val chapterId = location.section.owner.id
        val navigationRequest = state.navigationRequest
        if (!navigationRequest.acceptsLocation(chapterId, location.position, location.section.key)) return
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
        activityCheckpointJob?.cancel()
        activityCheckpointJob = null
    }

    private fun addLoadedSession(session: OpenedBookReaderSession, activate: Boolean) {
        val explicitSelection = chapterSelectionRequests.remove(session.chapter.id)
        val restoredSections = session.toDocumentSections(retainedSessions.locator(session.chapter.id)) ?: run {
            setLoadState(
                session.chapter.id,
                BookDocumentChapterLoadState.Failed(context.getString(R.string.book_document_incompatible)),
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
            ),
        )
        if (activate) activateChapter(session.chapter.id, completeForwardCrossing = false)
        if (explicitSelection) requestNavigation(sections.initialSection)
    }

    private fun requestNavigation(
        section: BookDocumentSection<EntryChapter>,
        position: mihon.book.api.document.BookDocumentPosition = section.initialPosition,
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
                childWebView = null,
                publicationNavigation = session.preparedPublication.publication.navigation,
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

    private fun setLoadState(chapterId: Long, loadState: BookDocumentChapterLoadState) {
        val state = currentState() ?: return
        updateState(state.copy(loadStates = state.loadStates + (chapterId to loadState)))
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
