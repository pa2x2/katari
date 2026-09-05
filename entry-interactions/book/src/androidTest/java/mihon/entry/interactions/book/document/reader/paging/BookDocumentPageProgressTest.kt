package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.book.document.reader.buildBookDocumentViewerItems
import mihon.entry.interactions.book.reader.BookReaderProgress
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.entry.model.EntryChapter

@RunWith(AndroidJUnit4::class)
class BookDocumentPageProgressTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun page_counter_follows_a_held_drag_and_return_without_persisting_unsettled_locations() {
        verifyLiveProgress(prependDuringDrag = false)
    }

    @Test fun loading_a_previous_chapter_during_a_drag_keeps_the_visible_page_and_counter() {
        verifyLiveProgress(prependDuringDrag = true)
    }

    private fun verifyLiveProgress(prependDuringDrag: Boolean) {
        val previous = pagingSection("Previous chapter.", 1)
        val section = pagingSection((1..100).joinToString(" ") { "Word$it paragraph text." }, 2)
        val next = pagingSection("Next chapter.", 3)
        val includePrevious = mutableStateOf(false)
        val initial = BookDocumentViewerLocation(section, section.initialPosition, 0f)
        var location = initial
        var progress: BookReaderProgress.Page? = null
        compose.setContent {
            PagingTheme {
                val items = buildBookDocumentViewerItems(
                    EntryChildWindow(
                        section.owner,
                        previous = previous.owner.takeIf {
                            includePrevious.value
                        },
                        next = next.owner,
                    ),
                    mapOf(1L to previous, 2L to section, 3L to next),
                    EntryChapter::id,
                )
                BookDocumentPaginationLayout(items, Modifier.size(280.dp, 300.dp).testTag("pager")) { pages ->
                    BookDocumentPagedViewer(
                        pages, BookDocumentReadingMode.PAGED_LTR, initial, null, emptyMap(),
                        0, 0, false, false, false, false,
                        { location = it }, {}, { _, _, _, _ -> }, { _, _ -> }, {}, {}, {}, {},
                        onPageProgress = { progress = it },
                    )
                }
            }
        }
        compose.waitForIdle()
        val count = requireNotNull(progress).totalPages
        assertTrue(count > 2)
        compose.onNodeWithTag("pager").performTouchInput {
            down(Offset(width * .9f, height * .5f))
            moveTo(Offset(width * .8f, height * .5f), 100)
            moveTo(Offset(width * .15f, height * .5f), 300)
        }
        compose.waitForIdle()
        assertEquals(
            "The next page is visible before the finger is lifted",
            BookReaderProgress.Page(2, count),
            progress,
        )
        assertEquals("A held gesture must not persist a reading position", initial.position, location.position)
        if (prependDuringDrag) {
            compose.runOnIdle { includePrevious.value = true }
            compose.waitForIdle()
            assertEquals(
                "Prepending content must not reset an active page turn",
                BookReaderProgress.Page(2, count),
                progress,
            )
            assertEquals(initial.position, location.position)
            compose.onNodeWithTag("pager").performTouchInput { up() }
            compose.waitForIdle()
            assertEquals(BookReaderProgress.Page(2, count), progress)
            assertTrue(
                "Settled navigation must still report after the window changes",
                location.position.offsetWithinBlock > 0,
            )
            return
        }
        compose.onNodeWithTag("pager").performTouchInput {
            moveTo(Offset(width * .8f, height * .5f), 300)
        }
        compose.waitForIdle()
        assertEquals("Returning the held gesture must update immediately", BookReaderProgress.Page(1, count), progress)
        compose.onNodeWithTag("pager").performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(initial.position, location.position)
    }
}
