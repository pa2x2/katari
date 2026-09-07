package mihon.entry.interactions.book.document.reader

import android.content.ClipboardManager
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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
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
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val copyLabel = composeRule.activity.getString(android.R.string.copy)
        val copyButton = By.text(copyLabel)
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
        if (showMenu) {
            assertTrue(
                "The native Copy action must appear after release",
                device.wait(Until.hasObject(copyButton), 5_000),
            )
        } else {
            assertFalse("The native menu must remain hidden", device.hasObject(copyButton))
        }

        // Selection handles are separate popup roots, outside the text's pointer-input tree.
        val handles = composeRule.onAllNodes(isPopup())
        val endHandleIndex = handles.fetchSemanticsNodes().indices.maxBy {
            handles[it].fetchSemanticsNode().positionOnScreen.x
        }
        val endHandle = handles[endHandleIndex]
        endHandle.performTouchInput { down(center) }
        assertTrue("The native menu must hide during a handle drag", device.wait(Until.gone(copyButton), 5_000))
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
        if (showMenu) {
            val selectedText = changes.last().text
            val copy = device.wait(Until.findObject(copyButton), 5_000)
            assertTrue("The native menu must return after handle release", copy != null)
            copy!!.click()
            composeRule.runOnIdle {
                val clipboard = composeRule.activity.getSystemService(ClipboardManager::class.java)
                assertEquals(selectedText, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
            }
            assertTrue("Copy must dismiss the native menu", device.wait(Until.gone(copyButton), 5_000))
            node.performTouchInput {
                down(wordPosition)
                advanceEventTime(1_000)
                moveTo(wordPosition)
            }
            composeRule.runOnIdle { assertFalse(changes.last().isSettled) }
            node.performTouchInput { up() }
            composeRule.runOnIdle { assertTrue(changes.last().isSettled) }
            assertTrue("A new selection must reopen the native menu", device.wait(Until.hasObject(copyButton), 5_000))
        }
    }
}
