package mihon.entry.interactions.book.document.reader

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class BookDocumentCompletionTrackerTest {
    @Test
    fun `forward chapter activation completes previous immediately even after fast scroll`() {
        val tracker = BookDocumentCompletionTracker<Long>()

        assertEquals(1L, tracker.onForwardChapterActivated(1L))
        assertNull(tracker.onForwardChapterActivated(1L))
    }

    @Test
    fun `brief or moving terminal visibility does not complete`() {
        val tracker = BookDocumentCompletionTracker<Long>()

        assertNull(tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = true))
        assertNull(tracker.onTerminalObservation(1L, false, canScrollForward = false, scrollInProgress = false))
    }

    @Test
    fun `repeated settled terminal visibility completes without elapsed time gate`() {
        val tracker = BookDocumentCompletionTracker<Long>()

        assertNull(tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = false))
        assertEquals(
            1L,
            tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = false),
        )
    }

    @Test
    fun `terminal completion is idempotent`() {
        val tracker = BookDocumentCompletionTracker<Long>()

        tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = false)
        tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = false)

        assertNull(tracker.onTerminalObservation(1L, true, canScrollForward = false, scrollInProgress = false))
    }
}
