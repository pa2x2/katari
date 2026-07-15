package mihon.entry.interactions.book

import java.util.UUID

/**
 * One-use, process-local handoff for already opened reader sessions.
 *
 * Intents only carry an opaque token and the serializable request needed for process-death fallback.
 */
internal class BookReaderSessionRegistry {
    private val sessions = mutableMapOf<String, OpenedBookReaderSession>()

    @Synchronized
    fun register(session: OpenedBookReaderSession): String {
        val token = UUID.randomUUID().toString()
        sessions[token] = session
        return token
    }

    @Synchronized
    fun claim(token: String, request: BookReaderRequest): OpenedBookReaderSession? {
        val session = sessions.remove(token) ?: return null
        if (session.entry.id == request.entryId && session.chapter.id == request.chapterId) {
            return session
        }
        session.close()
        return null
    }

    @Synchronized
    fun discard(token: String) {
        sessions.remove(token)?.close()
    }
}
