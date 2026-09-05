package mihon.entry.interactions.book.document.reader

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.book.document.render.toPreparedBookDocument
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tachiyomi.domain.entry.model.EntryChapter

@RunWith(AndroidJUnit4::class)
class BookDocumentReferenceSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun changing_the_note_target_scrolls_the_open_preview() {
        val html = (1..60).joinToString("") { "<p id='note-$it'>Note $it</p>" }
        val document = HtmlProseDocumentParser().parse(
            "notes",
            null,
            HtmlProseSanitizer.sanitize(html.encodeToByteArray()),
        )
        val section = BookDocumentSection(
            key = "notes",
            owner = EntryChapter.create().copy(id = 1),
            document = document.toPreparedBookDocument(),
            initialPosition = document.anchors.getValue("note-1"),
            resourceLoader = null,
        )
        val target = mutableStateOf(section)
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
                ) {
                    BookDocumentReferenceSheet(target.value, {}, { _, _ -> }, {})
                }
            }
        }
        composeRule.onNodeWithText("Note 1").assertIsDisplayed()
        composeRule.runOnIdle {
            target.value = section.copy(initialPosition = document.anchors.getValue("note-50"))
        }
        composeRule.onNodeWithText("Note 50").assertIsDisplayed()
    }

    @Test
    fun a_note_anchor_inside_a_long_block_is_visible() {
        val html = "<p>" + (1..100).joinToString("<br/>") { "Line $it before the note." } +
            "<br/><a id='target'></a>Target passage.<br/>End of note.</p>"
        val document = HtmlProseDocumentParser().parse(
            "notes",
            null,
            HtmlProseSanitizer.sanitize(html.encodeToByteArray()),
        )
        val section = BookDocumentSection(
            key = "long-note",
            owner = EntryChapter.create().copy(id = 1),
            document = document.toPreparedBookDocument(),
            initialPosition = document.anchors.getValue("target"),
            resourceLoader = null,
        )
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
                ) {
                    BookDocumentReferenceSheet(section, {}, { _, _ -> }, {})
                }
            }
        }
        val text = document.content.text.trimEnd('\n')
        val node = composeRule.onNodeWithText(text)
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val targetY = node.fetchSemanticsNode().positionInRoot.y +
            layouts.single().getBoundingBox(text.indexOf("Target passage.")).center.y
        assertTrue(
            "Target glyph must be on screen, was $targetY",
            targetY in 0f..composeRule.activity.window.decorView.height.toFloat(),
        )
    }
}
