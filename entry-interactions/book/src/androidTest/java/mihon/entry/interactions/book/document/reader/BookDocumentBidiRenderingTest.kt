package mihon.entry.interactions.book.document.reader

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class BookDocumentBidiRenderingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun an_inline_rtl_span_controls_neutral_characters_inside_an_ltr_paragraph() {
        val document = HtmlProseDocumentParser().parse(
            "page",
            null,
            HtmlProseSanitizer.sanitize(
                "<p dir='ltr'>Before <span dir='rtl'>(123)</span> after.</p>".encodeToByteArray(),
            ),
        )
        val block = document.blocks.single()
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
                    LocalBookDocumentSelectionChapterId provides 1L,
                ) {
                    BookDocumentBlockRenderer(block, document.content, "page", null, {}, {}, {})
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("Before", substring = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals(ResolvedTextDirection.Ltr, layout.getParagraphDirection(0))
        assertEquals(ResolvedTextDirection.Rtl, layout.getBidiRunDirection(layout.layoutInput.text.text.indexOf('(')))
    }
}
