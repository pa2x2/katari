package mihon.entry.interactions.book.document.reader.paging

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.BookDocumentModeViewport
import mihon.entry.interactions.book.document.reader.BookDocumentPublicationSections
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.viewer.EntryChildWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.entry.model.EntryChapter

@RunWith(AndroidJUnit4::class)
class BookDocumentModeSwitchTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun repeated_switches_preserve_the_first_visible_line_inside_a_long_paragraph() {
        verifyRoundTrips(pagingSection((1..200).joinToString(" ") { "Word$it follows the previous words." }))
    }

    @Test fun repeated_switches_preserve_the_top_paragraph_after_a_manual_page_turn() {
        verifyRoundTrips(
            pagingHtmlSection((1..40).joinToString("") { "<p>Paragraph $it has its own distinct words.</p>" }),
        )
    }

    private fun verifyRoundTrips(section: BookDocumentSection<EntryChapter>) {
        val mode = mutableStateOf(BookDocumentReadingMode.SCROLL)
        compose.setContent {
            PagingTheme {
                BookDocumentModeViewport(
                    currentChapter = section.owner,
                    currentChapterId = section.owner.id,
                    window = EntryChildWindow(section.owner),
                    loadedSections = mapOf(
                        section.owner.id to BookDocumentPublicationSections(listOf(section), section.key),
                    ),
                    loadStates = emptyMap(),
                    navigationRequest = null,
                    textSizePercent = 100,
                    onLocation = {},
                    onTransitionReached = {},
                    onTerminalObservation = { _, _, _, _ -> },
                    onAnchorMissing = {},
                    onInternalLinkClick = { _, _ -> },
                    onExternalLinkClick = {},
                    onScrollStarted = {},
                    onUserScrollStarted = {},
                    onReaderTap = {},
                    mode = mode.value,
                    tapZones = 0,
                    inversion = 0,
                    animation = false,
                    volume = false,
                    invertVolume = false,
                    chromeVisible = false,
                    modifier = Modifier.size(280.dp, 300.dp).testTag("viewport"),
                )
            }
        }
        compose.waitForIdle()
        val original = firstLines()
        compose.onNodeWithTag("viewport").performTouchInput { swipeUp() }
        compose.waitForIdle()
        val scrolled = firstLines()
        assertTrue("The setup must move to a different passage", original != scrolled)
        repeat(3) {
            compose.runOnIdle { mode.value = BookDocumentReadingMode.PAGED_VERTICAL }
            compose.waitForIdle()
            assertEquals("Scrolling to paging must retain the first visible lines (cycle $it)", scrolled, firstLines())
            compose.runOnIdle { mode.value = BookDocumentReadingMode.SCROLL }
            compose.waitForIdle()
            assertEquals(
                "Paging to scrolling must not center an old progress position (cycle $it)",
                scrolled,
                firstLines(),
            )
        }
        compose.runOnIdle { mode.value = BookDocumentReadingMode.PAGED_VERTICAL }
        compose.waitForIdle()
        compose.onNodeWithTag("viewport").performTouchInput { swipeUp() }
        compose.waitForIdle()
        val nextPage = firstLines()
        assertTrue("The page turn must reach another passage", nextPage != scrolled)
        compose.runOnIdle { mode.value = BookDocumentReadingMode.SCROLL }
        compose.waitForIdle()
        assertEquals("A page turn must replace the mode-switch anchor", nextPage, firstLines())
    }

    private fun firstLines(): List<String> {
        val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        return compose.onAllNodes(hasText("", substring = true), useUnmergedTree = true).fetchSemanticsNodes()
            .filter { node ->
                node.layoutInfo.isPlaced && node.boundsInRoot.width > 0 && node.boundsInRoot.height > 0 &&
                    generateSequence(node.parent) { it.parent }.none { it.config.isClearingSemantics }
            }
            .flatMap { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
                val text = node.config.getOrElse(SemanticsProperties.Text) { emptyList() }.joinToString("") { it.text }
                layouts.flatMap { layout ->
                    (0 until layout.lineCount).mapNotNull { line ->
                        val top = node.positionInRoot.y + layout.getLineTop(line)
                        val bottom = node.positionInRoot.y + layout.getLineBottom(line)
                        if (bottom <= viewport.top || top >= viewport.bottom) return@mapNotNull null
                        top to text.substring(layout.getLineStart(line), layout.getLineEnd(line)).trim()
                    }
                }
            }.sortedBy { it.first }.map { it.second }.filter { it.isNotEmpty() }.take(3)
            .also { assertEquals("Visible text must be available", 3, it.size) }
    }
}
