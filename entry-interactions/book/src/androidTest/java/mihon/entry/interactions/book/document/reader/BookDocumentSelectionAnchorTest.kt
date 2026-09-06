package mihon.entry.interactions.book.document.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentSelectionAnchorTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun reselecting_after_scroll_never_publishes_the_previous_text_position() {
        val text = (List(5) { "Opening line." } + "Translate this word." + List(50) { "Following line." })
            .joinToString("\n")
        val selections = mutableListOf<BookDocumentTextSelection.Changed>()
        lateinit var session: BookDocumentChapterSelection
        lateinit var scroll: ScrollState
        composeRule.setContent {
            BookDocumentSelectionFixture(
                text = text,
                onSelection = { if (it is BookDocumentTextSelection.Changed) selections += it },
                onSession = { selection, scrollState ->
                    session = selection
                    scroll = scrollState
                },
            )
        }
        val node = composeRule.onNodeWithText(text)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val wordPosition = layouts.single().getBoundingBox(text.indexOf("Translate") + 3).center
        node.performTouchInput { longClick(wordPosition) }
        val originalBounds = composeRule.runOnIdle {
            assertTrue(selections.isNotEmpty())
            assertEquals("Translate", selections.last().text)
            selections.last().boundsInReaderRoot
        }

        composeRule.runOnIdle { session.clearSelection() }
        composeRule.waitForIdle()
        val scrollDistance = 80
        composeRule.runOnIdle { runBlocking { scroll.scrollTo(scrollDistance) } }
        composeRule.runOnIdle { selections.clear() }
        node.performTouchInput { longClick(wordPosition - Offset(0f, scrollDistance.toFloat())) }

        composeRule.runOnIdle {
            assertTrue("The new selection must publish an anchor", selections.isNotEmpty())
            selections.forEach { selection ->
                assertEquals("Translate", selection.text)
                assertEquals(
                    "Every anchor must use the post-scroll position",
                    originalBounds.top - scrollDistance,
                    selection.boundsInReaderRoot.top,
                    0.5f,
                )
                assertEquals(originalBounds.bottom - scrollDistance, selection.boundsInReaderRoot.bottom, 0.5f)
            }
        }
    }
}
