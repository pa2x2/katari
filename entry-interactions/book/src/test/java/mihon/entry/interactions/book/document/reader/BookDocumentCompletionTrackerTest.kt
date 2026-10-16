package mihon.entry.interactions.book.document.reader

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BookDocumentCompletionTrackerTest {
    @Test
    fun `a chapter completes once, by forward activation or by consecutive settled chapter-end observations`() {
        val tracker = BookDocumentCompletionTracker<Long>()

        assertNull(tracker.onChapterEndObservation(1L, true, scrollInProgress = true))
        assertNull(tracker.onChapterEndObservation(1L, true, scrollInProgress = false))
        assertNull(tracker.onChapterEndObservation(1L, false, scrollInProgress = false))
        assertNull(tracker.onChapterEndObservation(1L, true, scrollInProgress = false))
        assertEquals(1L, tracker.onChapterEndObservation(1L, true, scrollInProgress = false))
        assertNull(tracker.onChapterEndObservation(1L, true, scrollInProgress = false))
        assertNull(tracker.onForwardChapterActivated(1L))
        assertEquals(2L, tracker.onForwardChapterActivated(2L))
        assertNull(tracker.onChapterEndObservation(2L, true, scrollInProgress = false))
        assertNull(tracker.onChapterEndObservation(2L, true, scrollInProgress = false))
    }
}
