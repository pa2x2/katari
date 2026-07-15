package mihon.entry.interactions.book.prose

import android.text.Layout
import android.text.TextPaint
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import tachiyomi.domain.entry.model.EntryChapter
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class HtmlProseDocumentTest {
    @Test
    fun `native prose parser preserves visible chapter content`() {
        val text = parseProseHtml("<h1>Chapter 1</h1><p>Hello <strong>reader</strong>.</p>")

        assertTrue(text.contains("Chapter 1"))
        assertTrue(text.contains("Hello reader."))
    }

    @Test
    fun `pagination fills multiple lines before advancing`() {
        val chapter = HtmlProseLoadedChapter(
            chapter = EntryChapter.create().copy(id = 1L, name = "Chapter 1"),
            resourceId = "chapter-1",
            bodyHtml = List(30) {
                "<p>Paragraph $it contains enough prose to wrap onto another line.</p>"
            }.joinToString(""),
            initialProgression = 0f,
        )
        val text = parseProseHtml(chapter.bodyHtml)
        val pages = paginateProse(
            chapter = chapter,
            text = text,
            paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = 20f },
            availableWidthPx = 320,
            availableHeightPx = 480,
            alignment = Layout.Alignment.ALIGN_NORMAL,
            lineSpacingMultiplier = 1.5f,
            justificationMode = Layout.JUSTIFICATION_MODE_NONE,
        )

        assertTrue(pages.size > 1)
        assertTrue(pages.first().text.lines().size > 1)
        assertEquals(pages.size, pages.last().total)
    }
}
