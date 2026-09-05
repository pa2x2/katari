package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentViewerLocation
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextScale
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentPagedRestorationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun page_turns_follow_tap_zones_and_reflow_keeps_the_current_passage_visible() {
        val section = pagingSection((1..100).joinToString(" ") { "Word$it paragraph text." })
        val initial = BookDocumentViewerLocation(section, section.initialPosition, 0f)
        var location = initial
        var pages = emptyList<BookDocumentPage>()
        val scale = mutableFloatStateOf(1f)
        compose.setContent {
            PagingTheme {
                CompositionLocalProvider(LocalBookDocumentTextScale provides scale.floatValue) {
                    BookDocumentPaginationLayout(
                        section.viewerBlocks,
                        Modifier.size(280.dp, 240.dp).testTag("pager"),
                    ) { measured ->
                        pages = measured
                        BookDocumentPagedViewer(
                            measured, BookDocumentReadingMode.PAGED_LTR, initial, null, emptyMap(),
                            0, 0, false, false, false, false,
                            { location = it }, {}, { _, _, _, _ -> }, { _, _ -> }, {}, {}, {}, {},
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("pager").performTouchInput { click(Offset(width * .9f, height * .5f)) }
        compose.waitForIdle()
        val afterTurn = location.position
        assertTrue(afterTurn.offsetWithinBlock > 0)
        compose.onNodeWithTag("pager").performTouchInput { click(Offset(width * .1f, height * .5f)) }
        compose.waitForIdle()
        assertEquals(0, location.position.offsetWithinBlock)
        compose.onNodeWithTag("pager").performTouchInput { swipeLeft() }
        compose.waitForIdle()
        val beforeReflow = location.position
        assertTrue(beforeReflow.offsetWithinBlock > 0)
        (11..20).forEach { step ->
            compose.runOnIdle { scale.floatValue = step / 10f }
            compose.waitForIdle()
        }
        compose.runOnIdle {
            val current = pages.first { it.contains(section.key, location.position) }
            assertTrue(current.contains(section.key, beforeReflow))
        }
    }
}
