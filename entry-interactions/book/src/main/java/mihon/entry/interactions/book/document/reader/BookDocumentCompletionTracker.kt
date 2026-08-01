package mihon.entry.interactions.book.document.reader

/**
 * Converts reader-owned navigation evidence into idempotent chapter completion events.
 *
 * Crossing into the next chapter is definitive and completes immediately. A terminal boundary is
 * only a fallback: it must remain visible across consecutive settled layout observations at the
 * hard end. No elapsed-time threshold gates either path.
 */
internal class BookDocumentCompletionTracker<K> {
    private val completed = mutableSetOf<K>()
    private var terminalCandidate: K? = null
    private var terminalObservationCount = 0

    fun onForwardChapterActivated(previous: K): K? = previous.takeIf(completed::add)

    fun onTerminalObservation(
        chapter: K,
        terminalBoundaryVisible: Boolean,
        canScrollForward: Boolean,
        scrollInProgress: Boolean,
    ): K? {
        if (!terminalBoundaryVisible || canScrollForward || scrollInProgress || chapter in completed) {
            clearTerminalCandidate()
            return null
        }
        if (terminalCandidate != chapter) {
            terminalCandidate = chapter
            terminalObservationCount = 1
            return null
        }
        terminalObservationCount++
        return chapter.takeIf { terminalObservationCount >= REQUIRED_TERMINAL_OBSERVATIONS && completed.add(it) }
    }

    private fun clearTerminalCandidate() {
        terminalCandidate = null
        terminalObservationCount = 0
    }

    private companion object {
        const val REQUIRED_TERMINAL_OBSERVATIONS = 2
    }
}
