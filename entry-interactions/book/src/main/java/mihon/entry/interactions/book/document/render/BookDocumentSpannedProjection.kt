package mihon.entry.interactions.book.document.render

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import mihon.book.api.document.BookDocumentBlock
import mihon.book.api.document.BookDocumentBlockKind
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentLink
import mihon.book.api.document.BookDocumentLinkTarget
import mihon.book.api.document.BookDocumentRichText

/**
 * Projects canonical semantic block text into Android's span representation.
 *
 * Publication-resource typefaces are applied later after validated resource loading.
 */
internal fun String.toBookDocumentSpanned(block: BookDocumentBlock): Spanned =
    toBookDocumentSpanned(block.links, block.inlineStyles).apply {
        if (isNotEmpty() && (block.style.bold || block.role.kind == BookDocumentBlockKind.HEADING)) {
            setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        if (isNotEmpty() && block.role.kind == BookDocumentBlockKind.HEADING) {
            val scale = when (block.role.level) {
                1 -> 1.5f
                2 -> 1.4f
                3 -> 1.3f
                4 -> 1.2f
                5 -> 1.1f
                else -> 1f
            }
            if (scale != 1f) {
                setSpan(
                    RelativeSizeSpan(scale),
                    0,
                    length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        }
        if (isNotEmpty() && block.role.kind == BookDocumentBlockKind.QUOTE) {
            setSpan(
                QuoteSpan(),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

internal fun BookDocumentRichText.toBookDocumentSpanned(): Spanned =
    text.toBookDocumentSpanned(links, inlineStyles)

private fun String.toBookDocumentSpanned(
    links: List<BookDocumentLink>,
    inlineStyles: List<BookDocumentInlineStyleRange>,
): SpannableString = SpannableString(this).apply {
    links.forEach { link ->
        val url = when (val target = link.target) {
            is BookDocumentLinkTarget.Anchor -> "#${target.fragment}"
            is BookDocumentLinkTarget.Resource -> buildString {
                append("book-resource:")
                append(target.resourceId)
                target.fragment?.let { fragment -> append('#').append(fragment) }
            }
            is BookDocumentLinkTarget.Reference -> buildString {
                append("book-reference:")
                target.resourceId?.let(::append)
                append('#').append(target.fragment)
            }
            is BookDocumentLinkTarget.External -> target.url
        }
        setSpan(
            URLSpan(url),
            link.start,
            link.endExclusive,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    inlineStyles.forEach { range ->
        val start = range.start.coerceIn(0, length)
        val end = range.endExclusive.coerceIn(start, length)
        if (end <= start) return@forEach
        val flags = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        val style = range.style
        style.fontSizeScale?.let {
            setSpan(RelativeSizeSpan(it), start, end, flags)
        }
        if (style.bold) setSpan(StyleSpan(Typeface.BOLD), start, end, flags)
        if (style.italic) setSpan(StyleSpan(Typeface.ITALIC), start, end, flags)
        if (style.underline) setSpan(UnderlineSpan(), start, end, flags)
        if (style.strikethrough) setSpan(StrikethroughSpan(), start, end, flags)
        if (style.subscript) setSpan(SubscriptSpan(), start, end, flags)
        if (style.superscript) setSpan(SuperscriptSpan(), start, end, flags)
        if (style.small && style.fontSizeScale == null) {
            setSpan(RelativeSizeSpan(SMALL_TEXT_SCALE), start, end, flags)
        }
        if (style.code && style.fontFamily == null) {
            setSpan(TypefaceSpan("monospace"), start, end, flags)
        }
        (style.fontFamily as? BookDocumentFontFamily.Generic)?.let { family ->
            val name = when (family.family) {
                BookDocumentFontFamily.GenericFamily.SERIF -> "serif"
                BookDocumentFontFamily.GenericFamily.SANS_SERIF -> "sans-serif"
                BookDocumentFontFamily.GenericFamily.MONOSPACE -> "monospace"
            }
            setSpan(TypefaceSpan(name), start, end, flags)
        }
    }
}

private const val SMALL_TEXT_SCALE = 0.8f
