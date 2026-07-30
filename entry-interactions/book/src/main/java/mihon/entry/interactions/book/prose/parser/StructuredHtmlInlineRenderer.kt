package mihon.entry.interactions.book.prose

import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.URLSpan
import androidx.core.text.HtmlCompat
import mihon.entry.interactions.book.document.model.BookDocumentInlineStyle
import mihon.entry.interactions.book.document.model.BookDocumentInlineStyleRange
import mihon.entry.interactions.book.document.model.BookDocumentLink
import mihon.entry.interactions.book.document.model.BookDocumentStyle
import mihon.entry.interactions.book.document.model.toBookDocumentLinkTarget
import org.jsoup.nodes.Element

internal data class AnchorMarker(
    val token: String,
    val fragments: List<String>,
)

internal data class InlineStyleMarker(
    val startToken: String,
    val endToken: String,
    val style: BookDocumentInlineStyle,
)

internal fun renderHtml(element: Element): RenderedFragment {
    val (anchored, anchorMarkers) = element.withAnchorMarkers()
    val (marked, styleMarkers) = anchored.withInlineStyleMarkers()
    val rendered = SpannableStringBuilder(
        HtmlCompat.fromHtml(marked.outerHtml(), HtmlCompat.FROM_HTML_MODE_LEGACY),
    )
    normalizeParagraphBreaks(rendered)
    val (anchorOffsets, inlineStyles) = rendered.removeDocumentMarkers(anchorMarkers, styleMarkers)
    if (!rendered.endsWith("\n\n")) rendered.append("\n\n")
    return RenderedFragment(SpannableString(rendered), anchorOffsets, inlineStyles)
}

internal fun renderPreservedText(element: Element): RenderedFragment {
    val (marked, markers) = element.withAnchorMarkers()
    val rendered = SpannableStringBuilder(marked.wholeText())
    val (anchorOffsets, _) = rendered.removeDocumentMarkers(markers, emptyList())
    return RenderedFragment(SpannableString(rendered), anchorOffsets).withParagraphTerminator()
}

internal fun Element.withAnchorMarkers(): Pair<Element, List<AnchorMarker>> {
    val marked = clone()
    val markers = buildList {
        val anchors = buildList {
            add(marked)
            marked.select("[id], a[name]").filterTo(this) { it !== marked }
        }
        anchors.forEach { anchor ->
            val fragments = anchor.ownFragments()
            if (fragments.isEmpty()) return@forEach
            val token = "$ANCHOR_MARKER_START${size.toString(36)}$ANCHOR_MARKER_END"
            anchor.prependText(token)
            add(AnchorMarker(token, fragments))
        }
    }
    return marked to markers
}

internal fun Element.withInlineStyleMarkers(): Pair<Element, List<InlineStyleMarker>> {
    val marked = clone()
    val markers = buildList {
        marked.select("*")
            .filterNot(Element::isBlockElement)
            .forEach { inline ->
                val style = inline.documentInlineStyle() ?: return@forEach
                val index = size.toString(36)
                val startToken = "$INLINE_STYLE_MARKER_START$index+$INLINE_STYLE_MARKER_END"
                val endToken = "$INLINE_STYLE_MARKER_START$index-$INLINE_STYLE_MARKER_END"
                inline.prependText(startToken)
                inline.appendText(endToken)
                add(InlineStyleMarker(startToken, endToken, style))
            }
    }
    return marked to markers
}

internal fun SpannableStringBuilder.removeDocumentMarkers(
    anchorMarkers: List<AnchorMarker>,
    styleMarkers: List<InlineStyleMarker>,
): Pair<Map<String, Int>, List<BookDocumentInlineStyleRange>> {
    val anchorPositions = anchorMarkers.mapNotNull { marker ->
        indexOf(marker.token).takeIf { it >= 0 }?.let { marker to it }
    }
    val stylePositions = styleMarkers.mapNotNull { marker ->
        val start = indexOf(marker.startToken)
        val end = indexOf(marker.endToken)
        if (start < 0 || end < start) null else Triple(marker, start, end)
    }
    val tokenRanges = buildList {
        anchorPositions.forEach { (marker, position) ->
            add(position until position + marker.token.length)
        }
        stylePositions.forEach { (marker, start, end) ->
            add(start until start + marker.startToken.length)
            add(end until end + marker.endToken.length)
        }
    }
    fun cleanedOffset(rawOffset: Int): Int =
        rawOffset - tokenRanges.filter { it.first < rawOffset }.sumOf(IntRange::count)

    val anchors = buildMap {
        anchorPositions.forEach { (marker, position) ->
            val offset = cleanedOffset(position)
            marker.fragments.forEach { putIfAbsent(it, offset) }
        }
    }
    val styles = stylePositions.mapNotNull { (marker, start, end) ->
        val cleanedStart = cleanedOffset(start)
        val cleanedEnd = cleanedOffset(end)
        if (cleanedEnd <= cleanedStart) {
            null
        } else {
            BookDocumentInlineStyleRange(cleanedStart, cleanedEnd, marker.style)
        }
    }
    tokenRanges.sortedByDescending(IntRange::first).forEach { range ->
        delete(range.first, range.last + 1)
    }
    return anchors to styles
}

internal fun Spanned.documentLinks(): List<BookDocumentLink> =
    getSpans(0, length, URLSpan::class.java).mapNotNull { span ->
        val target = span.url.toBookDocumentLinkTarget() ?: return@mapNotNull null
        BookDocumentLink(
            start = getSpanStart(span),
            endExclusive = getSpanEnd(span),
            target = target,
        )
    }

internal fun Spanned.withSemanticStyle(style: BookDocumentStyle): Spanned {
    if (!style.bold || isEmpty()) return this
    return SpannableString(this).apply {
        setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}
