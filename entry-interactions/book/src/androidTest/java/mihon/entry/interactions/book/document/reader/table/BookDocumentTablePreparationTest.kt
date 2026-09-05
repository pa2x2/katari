package mihon.entry.interactions.book.document.reader.table

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.book.api.document.BookDocumentBlockContent
import mihon.entry.interactions.book.document.reader.BOOK_DOCUMENT_BLOCK_HORIZONTAL_PADDING
import mihon.entry.interactions.book.document.reader.BookDocumentSection
import mihon.entry.interactions.book.document.reader.LocalBookDocumentSelectionChapterId
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextScale
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.book.document.render.toPreparedBookDocument
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDocumentTablePreparationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun prepared_rows_match_native_text_after_reading_width_and_text_size_change() {
        val text = "A long contents label that wraps across several lines in a narrow reading column."
        val document = HtmlProseDocumentParser().parse(
            "spacing",
            null,
            HtmlProseSanitizer.sanitize(
                "<table><tr><td><em>$text</em></td></tr><tr><td>Next row</td></tr></table>".encodeToByteArray(),
            ),
        )
        val block = document.blocks.single()
        val sections = listOf(
            BookDocumentSection(
                "spacing",
                1L,
                document.toPreparedBookDocument(),
                document.positionAtProgression(0f),
                null,
            ),
        )
        val scale = mutableFloatStateOf(1f)
        val width = mutableStateOf(240.dp)
        val shown = mutableStateOf(false)
        var cache: BookDocumentTableCache? = null
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
                    LocalBookDocumentSelectionChapterId provides 1L,
                    LocalBookDocumentTextScale provides scale.floatValue,
                ) {
                    BookDocumentTablePreparation(
                        sections,
                        Modifier.width(width.value + BOOK_DOCUMENT_BLOCK_HORIZONTAL_PADDING * 2),
                    ) {
                        val currentCache = LocalBookDocumentTableCache.current
                        SideEffect { cache = currentCache }
                        if (shown.value) {
                            Box(Modifier.padding(horizontal = BOOK_DOCUMENT_BLOCK_HORIZONTAL_PADDING)) {
                                BookDocumentTableRenderer(
                                    block.content as BookDocumentBlockContent.Table,
                                    block,
                                    "spacing",
                                    {},
                                    {},
                                )
                            }
                        }
                    }
                }
            }
        }
        for ((size, readingWidth) in listOf(1f to 240.dp, 1.6f to 240.dp, 1.6f to 320.dp)) {
            val previous = cache
            composeRule.runOnIdle {
                shown.value = false
                scale.floatValue = size
                width.value = readingWidth
            }
            val pixels = with(composeRule.density) {
                (readingWidth + BOOK_DOCUMENT_BLOCK_HORIZONTAL_PADDING * 2).roundToPx() -
                    BOOK_DOCUMENT_BLOCK_HORIZONTAL_PADDING.roundToPx() * 2
            }
            // Enter only after background preparation, exercising cached geometry rather than the fallback.
            composeRule.waitUntil(10_000) {
                (size == 1f || cache !== previous) && cache?.get(block, pixels, emptyMap()) != null
            }
            composeRule.runOnIdle { shown.value = true }
            val first = composeRule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
            val next = composeRule.onNodeWithText("Next row").fetchSemanticsNode().boundsInRoot
            val padding = with(composeRule.density) { 8.dp.roundToPx() * 2 }
            assertEquals(padding.toFloat(), next.top - first.bottom, 0.5f)
            assertEquals((pixels - padding).toFloat(), first.width, 0.5f)
        }
    }
}
