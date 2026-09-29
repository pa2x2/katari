package mihon.entry.interactions.book.document.reader

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BookDocumentCompletionTrackerTest {
    @Test
    fun `a chapter completes once, by forward activation or by consecutive settled terminal observations`() {
        val tracker = BookDocumentCompletionTracker<Long>()

        assertNull(tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = true))
        assertNull(tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = false))
        assertNull(tracker.onTerminalObservation(1L, false, canScrollForward = false, scrollInProgress = false))
        assertNull(tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = false))
        assertEquals(1L, tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = false))
        assertNull(tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = false))
        assertNull(tracker.onForwardChapterActivated(1L))
        assertEquals(2L, tracker.onForwardChapterActivated(2L))
        assertNull(tracker.onTerminalObservation(2L, true, canScrollForward = false, scrollInProgress = false))
        assertNull(tracker.onTerminalObservation(2L, true, canScrollForward = false, scrollInProgress = false))
    }
}
