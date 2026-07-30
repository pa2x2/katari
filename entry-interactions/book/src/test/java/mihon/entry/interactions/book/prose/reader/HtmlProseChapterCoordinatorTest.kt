package mihon.entry.interactions.book.prose

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class HtmlProseChapterCoordinatorTest {
    @Test
    fun `stale non-adjacent loads and duplicate active loads are rejected`() {
        assertFalse(
            shouldStartProseTransitionLoad(
                adjacent = false,
                loadActive = false,
                existingState = null,
                retry = false,
            ),
        )
        assertFalse(
            shouldStartProseTransitionLoad(
                adjacent = true,
                loadActive = true,
                existingState = null,
                retry = false,
            ),
        )
    }

    @Test
    fun `retry starts only from a failed transition state`() {
        assertFalse(
            shouldStartProseTransitionLoad(
                adjacent = true,
                loadActive = false,
                existingState = HtmlProseChapterLoadState.Loading,
                retry = true,
            ),
        )
        assertTrue(
            shouldStartProseTransitionLoad(
                adjacent = true,
                loadActive = false,
                existingState = HtmlProseChapterLoadState.Failed("failed"),
                retry = true,
            ),
        )
    }

    @Test
    fun `explicit selection resets the viewer without completing the current chapter`() {
        val explicit = proseChapterSwitchPolicy(
            currentIndex = 1,
            destinationIndex = 2,
            explicitSelection = true,
        )
        val boundary = proseChapterSwitchPolicy(
            currentIndex = 1,
            destinationIndex = 2,
            explicitSelection = false,
        )

        assertFalse(explicit.completeCurrent)
        assertTrue(explicit.resetViewer)
        assertTrue(boundary.completeCurrent)
        assertFalse(boundary.resetViewer)
    }
}
