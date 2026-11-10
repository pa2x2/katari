package app.katari.entry.interactions.book.reader

import app.katari.entry.interactions.book.processor.BookReaderRequest
import app.katari.entry.interactions.runtime.usage.ReadingSessionReport
import java.util.UUID

/**
 * One-use, process-local handoff for already opened reader sessions, with the usage report of the reader session they
 * start.
 *
 * Intents only carry an opaque token and the serializable request needed for process-death fallback.
 */
internal class BookReaderSessionRegistry {
    private val handoffs = mutableMapOf<String, BookReaderHandoff>()

    @Synchronized
    fun register(handoff: BookReaderHandoff): String {
        val token = UUID.randomUUID().toString()
        handoffs[token] = handoff
        return token
    }

    @Synchronized
    fun claim(token: String, request: BookReaderRequest): BookReaderHandoff? {
        val handoff = handoffs.remove(token) ?: return null
        val session = handoff.session
        if (session.entry.id == request.entryId && session.chapter.id == request.chapterId) {
            return handoff
        }
        session.close()
        return null
    }

    @Synchronized
    fun discard(token: String) {
        handoffs.remove(token)?.session?.close()
    }
}

/** A chapter the host opened, and the report of the session its opening started. */
internal class BookReaderHandoff(val session: OpenedBookReaderSession, val report: ReadingSessionReport)
