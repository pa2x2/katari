package mihon.entry.interactions.book.document.reader

/**
 * Converts reader-owned navigation evidence into idempotent chapter completion events.
 *
 * Crossing into the next chapter is definitive and completes immediately. Reaching a chapter's
 * end completes it too, so a chapter read to its end and then left still counts: the end must
 * remain visible across consecutive settled layout observations. No elapsed-time threshold gates
 * either path.
 */
internal class BookDocumentCompletionTracker<K> {
    private val completed = mutableSetOf<K>()
    private var endCandidate: K? = null
    private var endObservationCount = 0

    fun onForwardChapterActivated(previous: K): K? = previous.takeIf(completed::add)

    fun onChapterEndObservation(chapter: K, chapterEndVisible: Boolean, scrollInProgress: Boolean): K? {
        if (!chapterEndVisible || scrollInProgress || chapter in completed) {
            clearEndCandidate()
            return null
        }
        if (endCandidate != chapter) {
            endCandidate = chapter
            endObservationCount = 1
            return null
        }
        endObservationCount++
        return chapter.takeIf { endObservationCount >= REQUIRED_END_OBSERVATIONS && completed.add(it) }
    }

    private fun clearEndCandidate() {
        endCandidate = null
        endObservationCount = 0
    }

    private companion object {
        const val REQUIRED_END_OBSERVATIONS = 2
    }
}
