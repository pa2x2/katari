package mihon.entry.interactions.book.prose

import androidx.compose.ui.graphics.Color
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class HtmlProseReaderDocumentTest {
    private val palette = ProsePalette(
        background = Color.White,
        foreground = Color.Black,
        link = Color.Blue,
    )

    @Test
    fun `paginated document uses viewport columns and selected typography`() {
        val document = buildReaderDocument(
            bodyHtml = "<p>Chapter</p>",
            paginated = true,
            palette = palette,
            fontFamily = HtmlProseSettingsProvider.FONT_MONOSPACE,
            fontSizePercent = 120,
            lineHeightPercent = 160,
            pageMarginsPercent = 100,
            paragraphSpacingPercent = 75,
            textAlignment = HtmlProseSettingsProvider.ALIGN_JUSTIFY,
        )

        assertContains(document, "column-width: calc(100vw")
        assertContains(document, "overflow: hidden")
        assertContains(document, "font-family: monospace")
        assertContains(document, "font-size: 1.2rem")
        assertContains(document, "line-height: 1.6")
        assertContains(document, "text-align: justify")
        assertContains(document, "<body><p>Chapter</p></body>")
    }

    @Test
    fun `scrolling document uses a bounded reading column`() {
        val document = buildReaderDocument(
            bodyHtml = "<p>Chapter</p>",
            paginated = false,
            palette = palette,
            fontFamily = HtmlProseSettingsProvider.FONT_SERIF,
            fontSizePercent = 100,
            lineHeightPercent = 150,
            pageMarginsPercent = 100,
            paragraphSpacingPercent = 100,
            textAlignment = HtmlProseSettingsProvider.ALIGN_START,
        )

        assertContains(document, "max-width: 46rem")
        assertFalse(document.contains("column-width"))
    }
}
