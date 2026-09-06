package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentChapterLoadState
import mihon.entry.interactions.book.document.reader.BookDocumentViewerItem
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.entry.model.EntryChapter

@RunWith(AndroidJUnit4::class)
class BookDocumentTransitionPageTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun transition_stays_centered_when_the_reading_viewport_changes() {
        val page = transitionPage("Finished chapter", "Next chapter")
        val viewportSize = mutableStateOf(DpSize(280.dp, 500.dp))
        compose.setContent {
            PagingTheme {
                Box(Modifier.size(viewportSize.value).testTag("viewport")) {
                    BookDocumentPageContent(page, emptyMap(), { _, _ -> }, {}, {}, {})
                }
            }
        }
        listOf(DpSize(280.dp, 500.dp), DpSize(380.dp, 240.dp)).forEach { size ->
            compose.runOnIdle { viewportSize.value = size }
            compose.waitForIdle()
            val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
            val texts = compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes().filter { it.boundsInRoot.height > 0 }
            val textCenter = (texts.minOf { it.boundsInRoot.top } + texts.maxOf { it.boundsInRoot.bottom }) / 2
            assertEquals("Transition group must be vertically centered", viewport.center.y, textCenter, 2f)
            texts.forEach { text ->
                assertEquals(
                    "Transition text must be horizontally centered",
                    viewport.center.x,
                    text.boundsInRoot.center.x,
                    2f,
                )
            }
        }
    }

    @Test fun overflowing_transition_keeps_its_start_and_retry_action_accessible() {
        val title = "A long chapter title ".repeat(15).trim()
        val page = transitionPage(title, "Next chapter")
        var retried = false
        compose.setContent {
            PagingTheme {
                Box(Modifier.size(280.dp, 180.dp)) {
                    BookDocumentPageContent(
                        page,
                        mapOf(2L to BookDocumentChapterLoadState.Failed("Chapter could not load")),
                        { _, _ -> },
                        {},
                        {},
                        { retried = true },
                    )
                }
            }
        }
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithText("Retry").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(retried)
        compose.onNodeWithText(title).performScrollTo().assertIsDisplayed()
    }

    private fun transitionPage(fromTitle: String, toTitle: String): BookDocumentPage {
        val from = EntryChapter.create().copy(id = 1L, name = fromTitle)
        val to = EntryChapter.create().copy(id = 2L, name = toTitle)
        val item = BookDocumentViewerItem.Transition(EntryChildWindow(from, next = to).nextTransition(), "transition")
        return BookDocumentPage(
            listOf(BookDocumentPageFragment(item, firstOnPage = true, lastOnPage = true)),
            scrollable = true,
        )
    }
}
