package mihon.entry.interactions.book.document.reader.table

import androidx.activity.ComponentActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.entry.interactions.book.document.reader.BookDocumentChapterSelection
import mihon.entry.interactions.book.document.reader.BookDocumentChapterSelectionContainer
import mihon.entry.interactions.book.document.reader.BookDocumentTextInteraction
import mihon.entry.interactions.book.document.reader.BookDocumentTextSelection
import mihon.entry.interactions.book.document.reader.LocalBookDocumentSelectionChapterId
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextInteraction
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextScale
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentTableViewportTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun distant_rows_keep_links_and_selection_after_scroll_and_text_resize() {
        val html = buildString {
            append("<table><tr><th>Chapter</th><th>Description</th></tr>")
            repeat(200) { index ->
                append("<tr><td><a href='#chapter-$index'>Chapter $index</a></td>")
                append(
                    "<td>Marker$index A description that wraps across lines in the available column width.</td></tr>",
                )
            }
            append("</table><h2 id='chapter-199'>Destination</h2>")
        }
        val document = HtmlProseDocumentParser().parse(
            "long-table",
            null,
            HtmlProseSanitizer.sanitize(html.encodeToByteArray()),
        )
        val block = document.blocks.first()
        val table = block.content as BookDocumentBlockContent.Table
        val expectedLink = table.rows.last().cells.first().links.single().target
        var clicked: BookDocumentLinkTarget? = null
        lateinit var scroll: ScrollState
        lateinit var selection: BookDocumentChapterSelection
        var selectedText: String? = null
        val scale = mutableFloatStateOf(1f)
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
                    LocalBookDocumentSelectionChapterId provides 1L,
                    LocalBookDocumentTextScale provides scale.floatValue,
                    LocalBookDocumentTextInteraction provides BookDocumentTextInteraction.Disabled.copy(
                        observeSelections = true,
                        onSelection = { if (it is BookDocumentTextSelection.Changed) selectedText = it.text },
                    ),
                ) {
                    scroll = rememberScrollState()
                    BookDocumentChapterSelectionContainer(chapterId = 1L) { session ->
                        selection = session
                        Column(Modifier.width(360.dp).height(400.dp).verticalScroll(scroll)) {
                            BookDocumentTableRenderer(table, block, "table", { clicked = it }, {})
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithText("Chapter 0").assertIsDisplayed()
        composeRule.onNodeWithText("Chapter 199").assertDoesNotExist()
        for (size in listOf(1f, 1.6f)) {
            composeRule.runOnIdle { scale.floatValue = size }
            composeRule.runOnIdle { runBlocking { scroll.scrollTo(scroll.maxValue) } }
            composeRule.onNodeWithText("Chapter 199").assertIsDisplayed().performClick()
            composeRule.runOnIdle { assertEquals(expectedLink, clicked) }
            val layouts = mutableListOf<TextLayoutResult>()
            val lastRow = composeRule.onNodeWithText("Marker199", substring = true)
            lastRow.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            val number = layouts.single().getBoundingBox(3).center
            lastRow.performTouchInput { longClick(number) }
            composeRule.runOnIdle {
                assertEquals("Marker199", selectedText)
                selection.clearSelection()
                selectedText = null
            }
            composeRule.onNodeWithText("Chapter 0").assertDoesNotExist()
            composeRule.runOnIdle { runBlocking { scroll.scrollTo(0) } }
            composeRule.onNodeWithText("Chapter 0").assertIsDisplayed()
            composeRule.onNodeWithText("Chapter 199").assertDoesNotExist()
        }
    }
}
