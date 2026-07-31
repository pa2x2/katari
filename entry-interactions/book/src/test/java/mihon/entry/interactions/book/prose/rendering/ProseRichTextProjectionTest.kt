package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import mihon.book.api.document.BookDocumentInlineStyle
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentLink
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.BookDocumentRichText
import mihon.book.api.document.BookDocumentTextRange
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
internal class ProseRichTextProjectionTest {
    @Test
    fun `rich leaf projection preserves links and every inline semantic`() {
        val text = "styled sub super"
        val styledEnd = "styled".length
        val subStart = text.indexOf("sub")
        val superStart = text.indexOf("super")
        val richText = BookDocumentRichText(
            text = text,
            range = BookDocumentTextRange(0, text.length),
            links = listOf(
                BookDocumentLink(
                    start = 0,
                    endExclusive = styledEnd,
                    target = BookDocumentLinkTarget.External("https://example.invalid/"),
                ),
            ),
            inlineStyles = listOf(
                BookDocumentInlineStyleRange(
                    start = 0,
                    endExclusive = styledEnd,
                    style = BookDocumentInlineStyle(
                        italic = true,
                        underline = true,
                        strikethrough = true,
                        code = true,
                        small = true,
                    ),
                ),
                BookDocumentInlineStyleRange(
                    start = subStart,
                    endExclusive = subStart + "sub".length,
                    style = BookDocumentInlineStyle(subscript = true),
                ),
                BookDocumentInlineStyleRange(
                    start = superStart,
                    endExclusive = superStart + "super".length,
                    style = BookDocumentInlineStyle(superscript = true),
                ),
            ),
        )

        val projected = richText.toSpanned(inlineTypefaces = emptyMap())

        assertEquals(
            listOf("https://example.invalid/"),
            projected.getSpans(0, styledEnd, URLSpan::class.java).map(URLSpan::getURL),
        )
        assertTrue(
            projected.getSpans(0, styledEnd, StyleSpan::class.java)
                .any { it.style == Typeface.ITALIC },
        )
        assertTrue(projected.getSpans(0, styledEnd, UnderlineSpan::class.java).isNotEmpty())
        assertTrue(projected.getSpans(0, styledEnd, StrikethroughSpan::class.java).isNotEmpty())
        assertTrue(projected.getSpans(subStart, subStart + 3, SubscriptSpan::class.java).isNotEmpty())
        assertTrue(projected.getSpans(superStart, superStart + 5, SuperscriptSpan::class.java).isNotEmpty())
        assertEquals(
            listOf("monospace"),
            projected.getSpans(0, styledEnd, TypefaceSpan::class.java).map(TypefaceSpan::getFamily),
        )
        assertEquals(
            listOf(0.8f),
            projected.getSpans(0, styledEnd, RelativeSizeSpan::class.java)
                .map(RelativeSizeSpan::getSizeChange),
        )
    }
}
