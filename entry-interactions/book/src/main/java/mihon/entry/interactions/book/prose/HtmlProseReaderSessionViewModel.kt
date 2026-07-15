package mihon.entry.interactions.book.prose

import androidx.lifecycle.ViewModel
import mihon.book.api.BookLocator
import mihon.entry.interactions.book.OpenedBookReaderSession

/** Retains the active prose chapter and its small preloaded neighbor window across configuration changes. */
internal class HtmlProseReaderSessionViewModel : ViewModel() {
    private val sessions = linkedMapOf<Long, OpenedBookReaderSession>()
    private val locations = mutableMapOf<Long, BookLocator>()

    var currentSession: OpenedBookReaderSession? = null
        private set

    val currentLocator: BookLocator?
        get() = currentSession?.let { locations[it.chapter.id] ?: it.initialLocator }

    fun attachInitial(session: OpenedBookReaderSession) {
        check(currentSession == null) { "A prose reader session is already attached" }
        sessions[session.chapter.id] = session
        session.initialLocator?.let { locations[session.chapter.id] = it }
        currentSession = session
    }

    fun cache(session: OpenedBookReaderSession): Boolean {
        val current = currentSession ?: return false
        if (session.entry.id != current.entry.id) return false
        val existing = sessions.putIfAbsent(session.chapter.id, session)
        if (existing != null) return existing === session
        session.initialLocator?.let { locations[session.chapter.id] = it }
        return true
    }

    fun cached(chapterId: Long): OpenedBookReaderSession? = sessions[chapterId]

    fun locator(chapterId: Long): BookLocator? = locations[chapterId] ?: sessions[chapterId]?.initialLocator

    fun switchTo(chapterId: Long): OpenedBookReaderSession? {
        val destination = sessions[chapterId] ?: return null
        currentSession = destination
        return destination
    }

    fun updateLocation(locator: BookLocator) {
        currentSession?.let { locations[it.chapter.id] = locator }
    }

    fun retain(chapterIds: Set<Long>) {
        val currentId = currentSession?.chapter?.id
        sessions.keys.toList().forEach { chapterId ->
            if (chapterId != currentId && chapterId !in chapterIds) {
                sessions.remove(chapterId)?.close()
                locations.remove(chapterId)
            }
        }
    }

    fun release() {
        sessions.values.forEach(OpenedBookReaderSession::close)
        sessions.clear()
        locations.clear()
        currentSession = null
    }

    override fun onCleared() {
        release()
    }
}
