package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.book.document.reader.buildBookDocumentViewerItems
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.entry.model.EntryChapter

@RunWith(AndroidJUnit4::class)
class BookDocumentPagedNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun chapter_transition_is_a_separate_page_before_the_next_chapter() {
        val first = pagingSection("First chapter text.")
        val next = pagingSection("Next chapter text.", 2)
        val items = buildBookDocumentViewerItems(
            EntryChildWindow(first.owner, next = next.owner),
            mapOf(1L to first, 2L to next),
            EntryChapter::id,
        )
        val initial = BookDocumentViewerLocation(first, first.initialPosition, 0f)
        var location = initial
        var requestedChapter: Long? = null
        compose.setContent {
            PagingTheme {
                BookDocumentPaginationLayout(items, Modifier.size(280.dp, 400.dp).testTag("pager")) { pages ->
                    BookDocumentPagedViewer(
                        pages, BookDocumentReadingMode.PAGED_LTR, initial, null, emptyMap(),
                        0, 0, false, false, false, false,
                        { location = it }, { requestedChapter = it.id }, { _, _, _, _ -> }, { _, _ -> }, {}, {}, {}, {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("pager").performTouchInput { click(Offset(width * .9f, height * .8f)) }
        compose.waitForIdle()
        assertEquals(2L, requestedChapter)
        assertEquals(1L, location.section.owner.id)
        compose.onNodeWithTag("pager").performTouchInput { click(Offset(width * .9f, height * .8f)) }
        compose.waitForIdle()
        assertEquals(2L, location.section.owner.id)
    }

    @Test
    fun rtl_and_vertical_modes_follow_their_swipe_direction_and_keep_hardware_navigation() {
        val section = pagingSection((1..100).joinToString(" ") { "Word$it paragraph text." })
        val initial = BookDocumentViewerLocation(section, section.initialPosition, 0f)
        var location = initial
        val mode = androidx.compose.runtime.mutableStateOf(BookDocumentReadingMode.PAGED_RTL)
        compose.setContent {
            PagingTheme {
                BookDocumentPaginationLayout(
                    section.viewerBlocks,
                    Modifier.size(280.dp, 240.dp).testTag("pager"),
                ) { pages ->
                    BookDocumentPagedViewer(
                        pages, mode.value, initial, null, emptyMap(),
                        0, 0, false, false, false, false,
                        { location = it }, {}, { _, _, _, _ -> }, { _, _ -> }, {}, {}, {}, {},
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("pager").performTouchInput { swipeRight() }
        compose.waitForIdle()
        val rtlPosition = location.position.offsetWithinBlock
        assertTrue(rtlPosition > 0)
        compose.runOnIdle { mode.value = BookDocumentReadingMode.PAGED_VERTICAL }
        compose.waitForIdle()
        compose.onNodeWithTag("pager").performTouchInput { swipeUp() }
        compose.waitForIdle()
        val verticalPosition = location.position.offsetWithinBlock
        assertTrue(verticalPosition > rtlPosition)
        compose.onNodeWithTag("pager").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.PageDown) }
        compose.waitForIdle()
        assertTrue(location.position.offsetWithinBlock > verticalPosition)
    }
}
