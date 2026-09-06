package mihon.entry.interactions.book.document.reader

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.book.format.html.prosechapter.parsing.HtmlProseDocumentParser
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizationPolicy
import mihon.entry.interactions.book.format.html.prosechapter.sanitization.HtmlProseSanitizer
import mihon.entry.interactions.book.preparation.BookPublicationResource
import mihon.entry.interactions.book.preparation.BookPublicationResourceLoader
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class BookDocumentFontRenderingTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun authored_font_bytes_control_glyph_metrics_and_are_shared_between_text_leaves() {
        val bytes = File("/system/fonts/DroidSansMono.ttf").readBytes()
        val requests = AtomicInteger()
        val loader = object : BookPublicationResourceLoader {
            override suspend fun load(
                resourceId: String,
                acceptedMediaTypes: Set<String>,
                maxBytes: Int,
            ): Result<BookPublicationResource> {
                requests.incrementAndGet()
                return Result.success(BookPublicationResource(resourceId, "font/ttf", bytes))
            }
        }
        val body = HtmlProseSanitizer.sanitize(
            "<div style='font-family:reader-mono'><p>iiiiWWWW</p><p>Another leaf</p></div>".encodeToByteArray(),
            HtmlProseSanitizationPolicy(resolveFontResource = { family -> "font".takeIf { family == "reader-mono" } }),
        )
        val document = HtmlProseDocumentParser().parse("page", null, body)
        composeRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalBookDocumentReaderPalette provides bookDocumentReaderPalette(BookDocumentReaderThemeMode.APP),
                    LocalBookDocumentSelectionChapterId provides 1L,
                    LocalBookDocumentResourceLoader provides loader,
                ) {
                    Column {
                        document.blocks.forEach { block ->
                            BookDocumentBlockRenderer(block, document.content, "page", loader, {}, {}, {})
                        }
                    }
                }
            }
        }
        composeRule.waitUntil(5_000) {
            val layouts = mutableListOf<TextLayoutResult>()
            composeRule.onNodeWithText("iiiiWWWW").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
                it(layouts)
            }
            val layout = layouts.single()
            val narrow = layout.getHorizontalPosition(4, true) - layout.getHorizontalPosition(0, true)
            val wide = layout.getHorizontalPosition(8, true) - layout.getHorizontalPosition(4, true)
            abs(narrow - wide) < 0.5f
        }
        assertEquals(1, requests.get())
    }
}
