package mihon.entry.interactions.book.document.reader

import androidx.lifecycle.ViewModel
import mihon.book.api.BookLocator
import mihon.entry.interactions.book.reader.OpenedBookReaderSession

/** Retains the bounded document-reader session window across configuration changes. */
internal class BookDocumentReaderSessionViewModel : ViewModel() {
    private val sessions = linkedMapOf<Long, OpenedBookReaderSession>()
    private val locators = mutableMapOf<Long, BookLocator>()

    var currentChapterId: Long? = null
        private set

    fun attachInitial(session: OpenedBookReaderSession) {
        check(sessions.isEmpty()) { "An initial document reader session is already attached" }
        sessions[session.chapter.id] = session
        session.initialLocator?.let { locators[session.chapter.id] = it }
        currentChapterId = session.chapter.id
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
        return session
    }

    fun updateLocation(chapterId: Long, locator: BookLocator) {
        if (chapterId in sessions) locators[chapterId] = locator
    }

    fun locator(chapterId: Long): BookLocator? = locators[chapterId]

    fun retain(chapterIds: Set<Long>) {
        sessions.keys.toList()
            .filterNot(chapterIds::contains)
            .forEach { chapterId ->
                sessions.remove(chapterId)?.close()
                locators.remove(chapterId)
            }
    }

    fun release() {
        sessions.values.forEach(OpenedBookReaderSession::close)
        sessions.clear()
        locators.clear()
        currentChapterId = null
    }

    override fun onCleared() = release()
}
