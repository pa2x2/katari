package mihon.entry.interactions.book.prose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.book.BookReaderOpenResult
import mihon.entry.interactions.book.OpenedBookReaderSession
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPolicy
import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.entry.interactions.viewer.entryChildWindow
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.EntryChapter
import uy.kohesive.injekt.api.get

internal class HtmlProseChapterCoordinator(
    private val scope: CoroutineScope,
    private val retainedSession: HtmlProseReaderSessionViewModel,
    private val currentSession: () -> OpenedBookReaderSession?,
    private val currentState: () -> HtmlProseReaderUiState?,
    private val onStateChange: (HtmlProseReaderUiState?) -> Unit,
    private val prepareNextChapterEnabled: () -> Boolean,
    private val resolveChapters: suspend (OpenedBookReaderSession) -> List<EntryChapter>,
    private val openSession: suspend (EntryChapter, String) -> BookReaderOpenResult,
    private val beforeSwitch: suspend (completed: Boolean) -> Unit,
    private val onSessionActivated: suspend (OpenedBookReaderSession, resetViewer: Boolean) -> Unit,
    private val incompatibleSessionMessage: () -> String,
    private val chapterProjector: HtmlProseChapterProjector,
) : AutoCloseable {
    internal var navigation: EntryChildWindow<EntryChapter>? = null
    internal var chapters: List<EntryChapter> = emptyList()
    internal var processorId: String? = null
    private val chapterLoadJobs = mutableMapOf<Long, Job>()
    private var chapterSwitchJob: Job? = null

    private val openedSession get() = currentSession()
    private var uiState
        get() = currentState()
        set(value) {
            onStateChange(value)
        }

    internal suspend fun resolveWindow(session: OpenedBookReaderSession): EntryChildWindow<EntryChapter> {
        if (chapters.isEmpty()) {
            chapters = resolveChapters(session)
        }
        return requireNotNull(chapters.entryChildWindow(session.chapter.id, EntryChapter::id)) {
            "The selected prose chapter is missing from the reading order"
        }.also { navigation = it }
    }

    internal fun onSessionShown(progression: Double?) {
        retainAdjacentSessions()
        prepareNextChapterIfNeeded(progression)
    }

    private suspend fun showSession(session: OpenedBookReaderSession, resetViewer: Boolean = false) {
        onSessionActivated(session, resetViewer)
    }

    private fun incompatibleMessage(): String = incompatibleSessionMessage()

    override fun close() {
        chapterLoadJobs.values.forEach(Job::cancel)
        chapterLoadJobs.clear()
        chapterSwitchJob?.cancel()
        chapterSwitchJob = null
    }

    internal fun prepareNextChapterIfNeeded(progression: Double?) {
        if (
            !ReaderChapterPreparationPolicy.shouldPrepare(
                enabled = prepareNextChapterEnabled(),
                progression = progression ?: 0.0,
            )
        ) {
            return
        }
        navigation?.next?.let { requestTransitionChapter(it, retry = false) }
    }

    internal fun enterChapter(chapter: EntryChapter) {
        if (chapter.id == openedSession?.chapter?.id) return
        val currentIndex = chapters.indexOfFirst { it.id == openedSession?.chapter?.id }
        val destinationIndex = chapters.indexOfFirst { it.id == chapter.id }
        val policy = proseChapterSwitchPolicy(
            currentIndex = currentIndex,
            destinationIndex = destinationIndex,
            explicitSelection = false,
        )
        launchChapterSwitch(
            chapter = chapter,
            completeCurrent = policy.completeCurrent,
            resetViewer = policy.resetViewer,
        )
    }

    internal fun selectChapter(chapter: EntryChapter) {
        if (chapter.id == openedSession?.chapter?.id) {
            uiState = uiState?.copy(chapterListVisible = false)
            return
        }
        uiState = uiState?.copy(chapterListVisible = false)
        val policy = proseChapterSwitchPolicy(
            currentIndex = chapters.indexOfFirst { it.id == openedSession?.chapter?.id },
            destinationIndex = chapters.indexOfFirst { it.id == chapter.id },
            explicitSelection = true,
        )
        launchChapterSwitch(
            chapter = chapter,
            completeCurrent = policy.completeCurrent,
            resetViewer = policy.resetViewer,
        )
    }

    internal fun launchChapterSwitch(
        chapter: EntryChapter,
        completeCurrent: Boolean,
        resetViewer: Boolean,
    ) {
        if (chapterSwitchJob?.isActive == true) return
        uiState = uiState?.copy(loadingChapterId = chapter.id, loadError = null)
        chapterSwitchJob = scope.launch {
            try {
                switchChapter(chapter, completeCurrent, resetViewer)
            } finally {
                chapterSwitchJob = null
            }
        }
    }

    internal suspend fun switchChapter(
        chapter: EntryChapter,
        completeCurrent: Boolean,
        resetViewer: Boolean,
    ) {
        val id = chapter.id
        chapterLoadJobs[id]?.join()
        val cached = retainedSession.cached(id)
        val destination = cached ?: when (val result = openChapter(chapter)) {
            is BookReaderOpenResult.Failure -> {
                uiState = uiState?.copy(
                    loadingChapterId = null,
                    loadError = result.failure.message,
                )
                return
            }
            is BookReaderOpenResult.Success -> {
                if (!retainedSession.cache(result.session)) {
                    result.session.close()
                    return
                }
                result.session
            }
        }
        beforeSwitch(completeCurrent)
        retainedSession.switchTo(id) ?: return
        showSession(destination, resetViewer = resetViewer)
    }

    internal fun retainAdjacentSessions() {
        val adjacent = listOfNotNull(navigation?.previous, navigation?.next)
        retainedSession.retain(adjacent.mapTo(mutableSetOf()) { it.id })
        val retainedIds = adjacent.mapTo(mutableSetOf()) { it.id }.apply {
            openedSession?.chapter?.id?.let(::add)
        }
        chapterLoadJobs.keys.toList()
            .filterNot(retainedIds::contains)
            .forEach { chapterId ->
                chapterLoadJobs.remove(chapterId)?.cancel()
            }
        uiState = uiState?.copy(
            loadedChapters = uiState?.loadedChapters.orEmpty().filterKeys(retainedIds::contains),
            transitionLoadStates = uiState
                ?.transitionLoadStates
                .orEmpty()
                .filterKeys(retainedIds::contains),
        )
        adjacent.forEach { chapter ->
            retainedSession.cached(chapter.id)?.let { cached ->
                scheduleLoadedChapter(cached)
            }
        }
    }

    internal fun requestTransitionChapter(
        chapter: EntryChapter,
        retry: Boolean,
    ) {
        val adjacent = isAdjacent(chapter.id)
        if (!adjacent) return
        retainedSession.cached(chapter.id)?.let {
            scheduleLoadedChapter(it)
            return
        }
        val loadActive = chapterLoadJobs[chapter.id]?.isActive == true
        val existingState = uiState?.transitionLoadStates?.get(chapter.id)
        if (!shouldStartProseTransitionLoad(adjacent, loadActive, existingState, retry)) {
            return
        }
        setTransitionLoadState(chapter.id, HtmlProseChapterLoadState.Loading)
        chapterLoadJobs[chapter.id] = scope.launch {
            try {
                when (val result = openChapter(chapter)) {
                    is BookReaderOpenResult.Failure -> {
                        if (isAdjacent(chapter.id)) {
                            setTransitionLoadState(
                                chapter.id,
                                HtmlProseChapterLoadState.Failed(result.failure.message),
                            )
                        }
                    }
                    is BookReaderOpenResult.Success -> {
                        if (!isAdjacent(chapter.id) || !retainedSession.cache(result.session)) {
                            result.session.close()
                        } else {
                            addLoadedChapter(result.session)
                        }
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logcat(LogPriority.ERROR, error) { "Failed to load prose transition destination" }
                if (isAdjacent(chapter.id)) {
                    setTransitionLoadState(
                        chapter.id,
                        HtmlProseChapterLoadState.Failed(
                            error.message ?: incompatibleMessage(),
                        ),
                    )
                }
            } finally {
                chapterLoadJobs.remove(chapter.id)
            }
        }
    }

    internal fun isAdjacent(chapterId: Long): Boolean =
        listOfNotNull(navigation?.previous, navigation?.next).any { it.id == chapterId }

    internal fun setTransitionLoadState(
        chapterId: Long,
        state: HtmlProseChapterLoadState?,
    ) {
        val states = uiState?.transitionLoadStates.orEmpty().toMutableMap()
        if (state == null) {
            states.remove(chapterId)
        } else {
            states[chapterId] = state
        }
        uiState = uiState?.copy(transitionLoadStates = states)
    }

    internal suspend fun addLoadedChapter(session: OpenedBookReaderSession) {
        val retainedDocument = uiState?.loadedChapters?.get(session.chapter.id)?.document
        val projection = chapterProjector.project(
            owner = session.chapter,
            publication = session.preparedPublication,
            locator = retainedSession.locator(session.chapter.id),
            reusableDocument = retainedDocument,
        ) ?: return
        if (session.chapter.id != openedSession?.chapter?.id && !isAdjacent(session.chapter.id)) return
        uiState = uiState?.copy(
            loadedChapters = uiState?.loadedChapters.orEmpty() + (session.chapter.id to projection.chapter),
            loadingChapterId = null,
            transitionLoadStates = uiState
                ?.transitionLoadStates
                .orEmpty()
                .minus(session.chapter.id),
        )
    }

    private fun scheduleLoadedChapter(session: OpenedBookReaderSession) {
        val chapterId = session.chapter.id
        if (uiState?.loadedChapters?.containsKey(chapterId) == true) return
        if (chapterLoadJobs[chapterId]?.isActive == true) return
        setTransitionLoadState(chapterId, HtmlProseChapterLoadState.Loading)
        chapterLoadJobs[chapterId] = scope.launch {
            try {
                addLoadedChapter(session)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logcat(LogPriority.ERROR, error) { "Failed to project prose transition destination" }
                if (isAdjacent(chapterId)) {
                    setTransitionLoadState(
                        chapterId,
                        HtmlProseChapterLoadState.Failed(error.message ?: incompatibleMessage()),
                    )
                }
            } finally {
                chapterLoadJobs.remove(chapterId)
            }
        }
    }

    internal suspend fun openChapter(chapter: EntryChapter): BookReaderOpenResult {
        val session = openedSession
            ?: return BookReaderOpenResult.Failure(
                mihon.book.api.BookFailure(
                    mihon.book.api.BookFailureReason.CONTENT_UNAVAILABLE,
                    incompatibleMessage(),
                ),
            )
        val processor = processorId
            ?: return BookReaderOpenResult.Failure(
                mihon.book.api.BookFailure(
                    mihon.book.api.BookFailureReason.PROCESSOR_UNAVAILABLE,
                    incompatibleMessage(),
                ),
            )
        return openSession(chapter, processor)
    }
}
