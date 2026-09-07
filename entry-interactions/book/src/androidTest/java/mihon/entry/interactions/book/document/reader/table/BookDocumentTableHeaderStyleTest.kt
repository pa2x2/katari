package mihon.entry.interactions.book.document.reader.table

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.reader.LocalBookDocumentSelectionChapterId
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentTableHeaderStyleTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun header_emphasis_preserves_authored_flow_and_reserves_the_rendered_height() {
        val heading = "A long Latin heading that wraps across several lines in this narrow table"
        val document = HtmlProseDocumentParser().parse(
            "table",
            null,
            HtmlProseSanitizer.sanitize(
                """<table dir="rtl" lang="ar" style="line-height:2;text-indent:1em">
                    <tr><th>$heading</th></tr><tr><td>Following row</td></tr></table>""".encodeToByteArray(),
            ),
        )
        val block = document.blocks.single()
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
                    LocalBookDocumentSelectionChapterId provides 1L,
                ) {
                    Box(Modifier.width(280.dp)) {
                        BookDocumentTableRenderer(block.content as BookDocumentBlockContent.Table, block, "table", {
                        }, {})
                    }
                }
            }
        }
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(heading).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val style = layout.layoutInput.style
        assertTrue("The header must wrap to exercise row-height measurement", layout.lineCount > 1)
        assertEquals(FontWeight.Bold, style.fontWeight)
        assertEquals(ResolvedTextDirection.Rtl, layout.getParagraphDirection(0))
        assertEquals(style.fontSize * 2, style.lineHeight)
        assertEquals(style.fontSize, style.textIndent?.firstLine)
        assertEquals("ar", style.localeList?.get(0)?.language)
        val headingBounds = compose.onNodeWithText(heading).fetchSemanticsNode().boundsInRoot
        val followingBounds = compose.onNodeWithText("Following row").fetchSemanticsNode().boundsInRoot
        assertTrue("The following row must not overlap the styled header", followingBounds.top >= headingBounds.bottom)
    }
}
