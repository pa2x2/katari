package app.katari.entry.interactions.book.document.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import app.katari.book.api.BookLocator
import app.katari.entry.interactions.book.document.reader.navigation.BookDocumentJumpHistory
import app.katari.entry.interactions.book.reader.OpenedBookReaderSession
import app.katari.entry.interactions.reader.settings.BookDocumentReadingMode
import app.katari.entry.interactions.runtime.usage.ReadingSessionReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tachiyomi.domain.entry.model.Entry

/**
 * Retains the bounded document-reader session window while the reader is rebuilt, as on an Android configuration
 * change or when it reopens for new text rules. Only the current chapter outlives process death: sessions reopen from
 * persisted progress, but the reader must reopen the chapter it had moved on to, not the one it was launched with.
 * It also keeps the usage report of the reader session, which ends with it.
 */
internal class BookDocumentReaderSessionViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    val jumpHistory = BookDocumentJumpHistory()

    private val sessions = linkedMapOf<Long, OpenedBookReaderSession>()
    private val locators = mutableMapOf<Long, BookLocator>()

    var currentChapterId: Long? = null
        private set(value) {
            field = value
            savedState[CURRENT_CHAPTER_ID_KEY] = value
        }

    /** The chapter that was current before the process died; null in a fresh reader. */
    val restoredChapterId: Long?
        get() = savedState[CURRENT_CHAPTER_ID_KEY]

    private val mutableCurrentOwner = MutableStateFlow<Entry?>(null)

    private var report: ReadingSessionReport? = null

    /** The reading mode the reader shows, which the session's report ends with. */
    var readingMode: BookDocumentReadingMode? = null

    /** The entry the current chapter belongs to: in a merged book, the chapter's member rather than the opened entry. */
    val currentOwner: StateFlow<Entry?> = mutableCurrentOwner.asStateFlow()

    /**
     * Attaches the chapter the reader opens with. [report] is the session's unless it has one already, as when the
     * chapter opens again for new text rules within the same session.
     */
    fun attachInitial(session: OpenedBookReaderSession, report: ReadingSessionReport) {
        check(sessions.isEmpty()) { "An initial document reader session is already attached" }
        if (this.report == null) this.report = report.apply { sourceKnown(session.owner.source) }
        sessions[session.chapter.id] = session
        // A chapter opened again in place keeps where the reader was rather than its last saved position.
        if (session.chapter.id !in locators) session.initialLocator?.let { locators[session.chapter.id] = it }
        currentChapterId = session.chapter.id
        mutableCurrentOwner.value = session.owner
    }

    fun cache(session: OpenedBookReaderSession): Boolean {
        val existing = sessions[session.chapter.id]
        if (existing != null) return existing === session
        sessions[session.chapter.id] = session
        session.initialLocator?.let { locators[session.chapter.id] = it }
        return true
    }

    fun session(chapterId: Long): OpenedBookReaderSession? = sessions[chapterId]

    fun currentSession(): OpenedBookReaderSession? = currentChapterId?.let(sessions::get)

    fun activate(chapterId: Long): OpenedBookReaderSession? {
        val session = sessions[chapterId] ?: return null
        currentChapterId = chapterId
        mutableCurrentOwner.value = session.owner
        return session
    }

    fun updateLocation(chapterId: Long, locator: BookLocator) {
        if (chapterId in sessions) locators[chapterId] = locator
    }

    fun locator(chapterId: Long): BookLocator? = locators[chapterId]

    /** The reader shows text of [chapterId]. */
    fun shown(chapterId: Long) {
        sessions[chapterId]?.let { report?.shown(it.downloaded) }
    }

    fun finished(chapterId: Long) {
        report?.finished(chapterId)
    }

    fun retain(chapterIds: Set<Long>) {
        sessions.keys.toList()
            .filterNot(chapterIds::contains)
            .forEach { chapterId ->
                sessions.remove(chapterId)?.close()
                locators.remove(chapterId)
            }
    }

    /**
     * Closes every session so its chapter opens again, as it must for new text rules to apply, keeping the current
     * chapter and where it was.
     */
    fun closeSessionsToReopen() {
        sessions.values.forEach(OpenedBookReaderSession::close)
        sessions.clear()
        locators.keys.retainAll(setOfNotNull(currentChapterId))
    }

    fun release() {
        sessions.values.forEach(OpenedBookReaderSession::close)
        sessions.clear()
        locators.clear()
        currentChapterId = null
        mutableCurrentOwner.value = null
    }

    override fun onCleared() {
        readingMode?.let { report?.endReading(it) }
        release()
    }

    private companion object {
        const val CURRENT_CHAPTER_ID_KEY = "current_chapter_id"
    }
}
