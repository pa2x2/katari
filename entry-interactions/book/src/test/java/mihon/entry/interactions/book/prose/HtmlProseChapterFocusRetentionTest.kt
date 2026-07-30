package mihon.entry.interactions.book.prose

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlProseChapterFocusRetentionTest {
    @Test
    fun `adjacent chapter rotation retains focus only for a surviving text section`() {
        val retainedSections = setOf("previous", "current", "next")

        assertTrue(
            shouldRetainProseTextFocus(
                focusedSectionKey = "next",
                retainedSectionKeys = retainedSections,
                resetViewer = false,
            ),
        )
        assertFalse(
            shouldRetainProseTextFocus(
                focusedSectionKey = "outgoing",
                retainedSectionKeys = retainedSections,
                resetViewer = false,
            ),
        )
        assertFalse(
            shouldRetainProseTextFocus(
                focusedSectionKey = "next",
                retainedSectionKeys = retainedSections,
                resetViewer = true,
            ),
        )
    }
}
