package mihon.entry.interactions.book.document.reader.paging

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

@RunWith(AndroidJUnit4::class)
class BookDocumentPagedSelectionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selection_dismissal_does_not_turn_the_page_and_the_following_tap_still_renders_text() {
        val section = pagingSection((1..50).joinToString(" ") { "Word$it paragraph text." })
        val initial = BookDocumentViewerLocation(section, section.initialPosition, 0f)
        var location = initial
        var selection: BookDocumentTextSelection.Changed? = null
        compose.setContent {
            PagingTheme {
                CompositionLocalProvider(
                    LocalBookDocumentTextInteraction provides
                        BookDocumentTextInteraction.Disabled.copy(
                            observeSelections = true,
                            onSelection = { selection = it as? BookDocumentTextSelection.Changed },
                        ),
                ) {
                    BookDocumentPaginationLayout(
                        section.viewerBlocks,
                        Modifier.size(280.dp, 240.dp).testTag("pager"),
                    ) { pages ->
                        BookDocumentPagedViewer(
                            pages, BookDocumentReadingMode.PAGED_LTR, initial, null, emptyMap(),
                            0, 0, false, false, false, false,
                            { location = it }, {}, { _, _, _, _ -> }, { _, _ -> }, {}, {}, {}, {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("pager").performTouchInput { longClick(Offset(width * .35f, height * .08f)) }
        compose.waitUntil(5000) { selection != null }
        compose.onNodeWithTag("pager").performTouchInput { click(Offset(width * .9f, height * .5f)) }
        compose.waitForIdle()
        assertEquals(0, location.position.offsetWithinBlock)
        assertEquals(null, selection)
        compose.onNodeWithTag("pager").performTouchInput {
            advanceEventTime(500)
            click(Offset(width * .9f, height * .5f))
        }
        compose.waitForIdle()
        assertTrue(location.position.offsetWithinBlock > 0)
        compose.onAllNodes(androidx.compose.ui.test.hasText("paragraph", substring = true))
            .fetchSemanticsNodes().also { assertTrue(it.isNotEmpty()) }
    }
}
