package mihon.entry.interactions.book.document.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentSelectionSettlementTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selection_settles_only_on_release_with_menu_hidden() = verifySelectionRelease(showMenu = false)

    @Test
    fun selection_settles_only_on_release_with_menu_visible() = verifySelectionRelease(showMenu = true)

    private fun verifySelectionRelease(showMenu: Boolean) {
        val text = "Select these words and then release the selection handle."
        val changes = mutableListOf<BookDocumentTextSelection.Changed>()
        composeRule.setContent {
            BookDocumentSelectionFixture(
                text = text,
                showTextSelectionMenu = showMenu,
                onSelection = { if (it is BookDocumentTextSelection.Changed) changes += it },
            )
        }
        val node = composeRule.onNodeWithText(text)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val wordPosition = layouts.single().getBoundingBox(text.indexOf("these") + 2).center

        node.performTouchInput {
            down(wordPosition)
            advanceEventTime(1_000)
            moveTo(wordPosition)
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue(changes.isNotEmpty())
            assertEquals("these", changes.last().text)
            assertTrue("A held long press must never settle", changes.none { it.isSettled })
        }
        node.performTouchInput { up() }
        composeRule.runOnIdle { assertTrue(changes.last().isSettled) }

        // Selection handles are separate popup roots, outside the text's pointer-input tree.
        val handles = composeRule.onAllNodes(isPopup())
        val endHandleIndex = handles.fetchSemanticsNodes().indices.maxBy {
            handles[it].fetchSemanticsNode().positionOnScreen.x
        }
        val endHandle = handles[endHandleIndex]
        endHandle.performTouchInput { down(center) }
        composeRule.runOnIdle {
            assertFalse("Picking up a handle must unsettle unchanged text", changes.last().isSettled)
            changes.clear()
        }
        endHandle.performTouchInput {
            moveBy(Offset(100f, 0f))
            advanceEventTime(1_000)
        }
        composeRule.runOnIdle {
            assertTrue(changes.isNotEmpty())
            assertTrue("Pausing a handle drag must never settle", changes.none { it.isSettled })
        }
        endHandle.performTouchInput { up() }
        composeRule.runOnIdle {
            assertTrue(changes.last().isSettled)
            assertTrue(
                "Expected an expanded selection, got ${changes.last().text}",
                changes.last().text.length > "these".length,
            )
        }
    }
}
