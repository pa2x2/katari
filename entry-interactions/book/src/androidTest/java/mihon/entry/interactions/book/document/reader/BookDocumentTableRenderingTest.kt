package mihon.entry.interactions.book.document.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.book.api.document.BookDocumentBlockContent
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
class BookDocumentTableRenderingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun single_cell_prose_uses_the_available_reading_width() {
        val text = "This ebook was one of Project Gutenberg's early files. " +
            "There is an improved illustrated edition of this title."
        val document = HtmlProseDocumentParser().parse(
            "notice",
            null,
            HtmlProseSanitizer.sanitize("<table><tr><td>$text</td></tr></table>".encodeToByteArray()),
        )
        val block = document.blocks.single()
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
                    LocalBookDocumentSelectionChapterId provides 1L,
                ) {
                    Box(Modifier.width(360.dp)) {
                        BookDocumentTableRenderer(
                            content = block.content as BookDocumentBlockContent.Table,
                            block = block,
                            selectionIdentity = "notice",
                            onAnchorClick = {},
                            onExternalLinkClick = {},
                        )
                    }
                }
            }
        }
        // The cell fills the viewport with its 8dp padding on either side, rather than a fixed column cap.
        composeRule.onNodeWithText(text).assertWidthIsEqualTo(344.dp)
    }

    @Test
    fun spanning_cells_keep_following_rows_in_their_columns_after_text_resize() {
        val document = HtmlProseDocumentParser().parse(
            "table",
            null,
            HtmlProseSanitizer.sanitize(
                """<table><tr><th rowspan="2">Languages</th><th>Word</th><th>Language</th></tr>
                <tr><td>PEHEE-NUEE-NUEE,</td><td>Erromangoan.</td></tr>
                <tr><td colspan="2">Following row</td><td>End</td></tr></table>""".encodeToByteArray(),
            ),
        )
        val block = document.blocks.single()
        val scale = mutableFloatStateOf(1f)
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
                    LocalBookDocumentSelectionChapterId provides 1L,
                    LocalBookDocumentTextScale provides scale.floatValue,
                ) {
                    BookDocumentTableRenderer(
                        block.content as BookDocumentBlockContent.Table,
                        block,
                        "table",
                        {},
                        {},
                    )
                }
            }
        }
        for (size in listOf(1f, 1.6f)) {
            composeRule.runOnIdle { scale.floatValue = size }
            fun bounds(text: String) = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
            val heading = bounds("Word")
            val word = bounds("PEHEE-NUEE-NUEE,")
            val language = bounds("Erromangoan.")
            val following = bounds("Following row")
            assertEquals(heading.left, word.left, 0.5f)
            assertEquals(word.top, language.top, 0.5f)
            assertTrue(word.left > bounds("Languages").left)
            assertTrue(following.top >= maxOf(word.bottom, language.bottom))
            assertEquals(following.top, bounds("End").top, 0.5f)
        }
    }
}
