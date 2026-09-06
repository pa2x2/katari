package mihon.entry.interactions.book.document.reader.paging

import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.BookDocumentTextInteraction
import mihon.entry.interactions.book.document.reader.BookDocumentTextSelection
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextInteraction
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.entry.model.EntryChapter

@RunWith(AndroidJUnit4::class)
class BookDocumentPagedSelectionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun volume_keys_keep_turning_pages_after_text_selection_is_dismissed() {
        val section = pagingSection((1..100).joinToString(" ") { "Word$it paragraph text." })
        val state = renderReader(section, volumeKeys = true)
        compose.onNodeWithTag("pager").performTouchInput { longClick(Offset(width * .35f, height * .08f)) }
        compose.waitUntil(5_000) { state.selection != null }
        compose.onNodeWithTag("pager").performTouchInput { click(Offset(width * .9f, height * .5f)) }
        compose.waitForIdle()
        assertEquals(null, state.selection)
        assertEquals(0, state.location.position.offsetWithinBlock)

        repeat(3) {
            val previousOffset = state.location.position.offsetWithinBlock
            pressVolumeKey(KeyEvent.KEYCODE_VOLUME_DOWN)
            compose.waitForIdle()
            assertTrue(
                "Volume down press ${it + 1} must keep turning pages",
                state.location.position.offsetWithinBlock > previousOffset,
            )
        }
        val previousOffset = state.location.position.offsetWithinBlock
        pressVolumeKey(KeyEvent.KEYCODE_VOLUME_UP)
        compose.waitForIdle()
        assertTrue("Volume up must still turn back", state.location.position.offsetWithinBlock < previousOffset)
    }

    @Test
    fun selection_can_restart_on_new_and_revisited_pages() {
        val section = pagingHtmlSection(
            "<html><body>" + (1..50).joinToString("") { index ->
                "<p>Paragraph $index with <a href='https://example.com/$index'>a link</a> and " +
                    (1..12).joinToString(" ") { "word$it" } + ".</p>"
            } + "</body></html>",
        )
        val state = renderReader(section, volumeKeys = true, animatePages = true)
        repeat(4) { visit ->
            repeat(2) {
                compose.onNodeWithTag("pager").performTouchInput {
                    longClick(Offset(width * .35f, height * .08f))
                }
                compose.waitUntil(5_000) { state.selection?.isSettled == true }
                assertTrue("Visit $visit must select visible text", !state.selection?.text.isNullOrBlank())
                compose.onNodeWithTag("pager").performTouchInput { click(Offset(width * .9f, height * .5f)) }
                compose.waitForIdle()
                assertEquals(null, state.selection)
            }
            compose.onNodeWithTag("pager").performTouchInput {
                longClick(Offset(width * .35f, height * .08f))
            }
            compose.waitUntil(5_000) { state.selection?.isSettled == true }
            val previousPosition = state.location.position
            pressVolumeKey(if (visit % 3 == 2) KeyEvent.KEYCODE_VOLUME_UP else KeyEvent.KEYCODE_VOLUME_DOWN)
            compose.waitForIdle()
            assertTrue(state.location.position != previousPosition)
        }
    }

    private fun pressVolumeKey(keyCode: Int) {
        // Dispatch through the activity without requesting focus on the pager for the test.
        compose.runOnIdle {
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            compose.activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
        }
    }

    @Test
    fun selection_dismissal_does_not_turn_the_page_and_the_following_tap_still_renders_text() {
        val section = pagingSection((1..50).joinToString(" ") { "Word$it paragraph text." })
        val state = renderReader(section, volumeKeys = false)
        compose.onNodeWithTag("pager").performTouchInput { longClick(Offset(width * .35f, height * .08f)) }
        compose.waitUntil(5000) { state.selection != null }
        compose.onNodeWithTag("pager").performTouchInput { click(Offset(width * .9f, height * .5f)) }
        compose.waitForIdle()
        assertEquals(0, state.location.position.offsetWithinBlock)
        assertEquals(null, state.selection)
        compose.onNodeWithTag("pager").performTouchInput {
            advanceEventTime(500)
            click(Offset(width * .9f, height * .5f))
        }
        compose.waitForIdle()
        assertTrue(state.location.position.offsetWithinBlock > 0)
        compose.onAllNodes(androidx.compose.ui.test.hasText("paragraph", substring = true))
            .fetchSemanticsNodes().also { assertTrue(it.isNotEmpty()) }
    }
    private fun renderReader(
        section: BookDocumentSection<EntryChapter>,
        volumeKeys: Boolean,
        animatePages: Boolean = false,
    ): SelectionReaderState {
        val initial = BookDocumentViewerLocation(section, section.initialPosition, 0f)
        val state = SelectionReaderState(initial)
        var ready = false
        compose.setContent {
            PagingTheme {
                CompositionLocalProvider(
                    LocalBookDocumentTextInteraction provides
                        BookDocumentTextInteraction.Disabled.copy(
                            observeSelections = true,
                            onSelection = { state.selection = it as? BookDocumentTextSelection.Changed },
                        ),
                ) {
                    BookDocumentPaginationLayout(
                        section.viewerBlocks,
                        Modifier.size(280.dp, 240.dp).testTag("pager"),
                    ) { pages ->
                        BookDocumentPagedViewer(
                            pages, BookDocumentReadingMode.PAGED_LTR, initial, null, emptyMap(),
                            0, 0, animatePages, volumeKeys, false, false,
                            { state.location = it }, {}, { _, _, _, _ -> }, { _, _ -> }, {}, {}, {}, {},
                            onPageProgress = { ready = it != null },
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.waitUntil(5_000) { ready }
        return state
    }

    private class SelectionReaderState(initial: BookDocumentViewerLocation<EntryChapter>) {
        var location = initial
        var selection: BookDocumentTextSelection.Changed? = null
    }
}
