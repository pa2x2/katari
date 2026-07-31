package mihon.entry.interactions.book.prose

import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import mihon.book.api.document.BookDocumentAlignment
import mihon.book.api.document.BookDocumentBlockContent
import mihon.book.api.document.BookDocumentFontFamily
import mihon.book.api.document.BookDocumentInlineStyle
import mihon.book.api.document.BookDocumentInlineStyleRange
import mihon.book.api.document.BookDocumentPosition
import mihon.book.api.document.BookDocumentStyle
import mihon.book.api.document.BookDocumentWhiteSpace
import mihon.entry.interactions.book.document.reader.applyBookDocumentTextLayoutPolicy
import mihon.entry.interactions.book.document.render.PreparedBookDocumentBlock

internal fun paginateProse(
    chapter: HtmlProseLoadedChapter,
    text: Spanned,
    paint: TextPaint,
    availableWidthPx: Int,
    availableHeightPx: Int,
    alignment: Layout.Alignment,
    lineSpacingMultiplier: Float,
    justificationMode: Int = Layout.JUSTIFICATION_MODE_NONE,
): List<HtmlProsePage> {
    if (text.isEmpty() || availableWidthPx <= 0 || availableHeightPx <= 0) return emptyList()
    val layout = buildProseLayout(
        text = text,
        paint = paint,
        availableWidthPx = availableWidthPx,
        alignment = alignment,
        lineSpacingMultiplier = lineSpacingMultiplier,
        justificationMode = justificationMode,
    )
    if (layout.lineCount == 0) return emptyList()

    val slices = buildList {
        var firstLine = 0
        while (firstLine < layout.lineCount) {
            val pageBottom = layout.getLineTop(firstLine) + availableHeightPx
            var lastLine = layout.getLineForVertical(pageBottom - 1)
                .coerceIn(firstLine, layout.lineCount - 1)
            while (lastLine > firstLine && layout.getLineBottom(lastLine) > pageBottom) {
                lastLine--
            }
            val start = layout.getLineStart(firstLine)
            var end = layout.getLineEnd(lastLine).coerceIn(start, text.length)
            var pageText = text.pageSlice(start, end)
            while (
                lastLine > firstLine &&
                buildProseLayout(
                    text = pageText,
                    paint = paint,
                    availableWidthPx = availableWidthPx,
                    alignment = alignment,
                    lineSpacingMultiplier = lineSpacingMultiplier,
                    justificationMode = justificationMode,
                ).height > availableHeightPx
            ) {
                lastLine--
                end = layout.getLineEnd(lastLine).coerceIn(start, text.length)
                pageText = text.pageSlice(start, end)
            }
            if (end > start) {
                add(
                    ProsePageSlice(
                        sourceRange = start until end,
                        text = pageText,
                    ),
                )
            }
            firstLine = lastLine + 1
        }
    }
    return slices.mapIndexed { index, slice ->
        HtmlProsePage(
            chapter = chapter.owner,
            index = index,
            total = slices.size,
            text = slice.text,
            progression = if (index == slices.lastIndex) {
                1f
            } else {
                slice.sourceRange.first.toFloat().div(text.length.coerceAtLeast(1))
            },
            sourceStart = slice.sourceRange.first,
            sourceEndExclusive = slice.sourceRange.last + 1,
        )
    }
}

private fun buildProseLayout(
    text: Spanned,
    paint: TextPaint,
    availableWidthPx: Int,
    alignment: Layout.Alignment,
    lineSpacingMultiplier: Float,
    justificationMode: Int,
): StaticLayout {
    val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, availableWidthPx)
        .applyBookDocumentTextLayoutPolicy()
        .setAlignment(alignment)
        .setLineSpacing(0f, lineSpacingMultiplier)
    if (justificationMode != Layout.JUSTIFICATION_MODE_NONE) {
        builder.setJustificationMode(justificationMode)
    }
    return builder.build()
}

private fun Spanned.pageSlice(start: Int, endExclusive: Int): Spanned =
    SpannableString(subSequence(start, endExclusive))

private data class ProsePageSlice(
    val sourceRange: IntRange,
    val text: Spanned,
)

internal fun paginateStructuredProse(
    chapter: HtmlProseLoadedChapter,
    paint: TextPaint,
    availableWidthPx: Int,
    availableHeightPx: Int,
    alignment: Layout.Alignment,
    lineSpacingMultiplier: Float,
    justificationMode: Int = Layout.JUSTIFICATION_MODE_NONE,
    resourceTypefaces: Map<String, Typeface> = emptyMap(),
): List<HtmlProsePage> {
    if (availableWidthPx <= 0 || availableHeightPx <= 0) return emptyList()
    val provisional = mutableListOf<HtmlProsePage>()
    val textRun = mutableListOf<PreparedBookDocumentBlock>()

    fun flushTextRun() {
        if (textRun.isEmpty()) return
        val sourceStart = textRun.first().block.logicalStart
        val text = SpannableStringBuilder().apply {
            textRun.forEach { prepared ->
                append(
                    prepared.renderedText.withPaginationStyles(
                        blockStyle = prepared.block.style,
                        inlineStyles = prepared.block.inlineStyles,
                        resourceTypefaces = resourceTypefaces,
                    ),
                )
            }
        }
        val pages = paginateProse(
            chapter = chapter,
            text = text,
            paint = paint,
            availableWidthPx = availableWidthPx,
            availableHeightPx = availableHeightPx,
            alignment = alignment,
            lineSpacingMultiplier = lineSpacingMultiplier,
            justificationMode = justificationMode,
        )
        pages.forEach { page ->
            provisional += page.copy(
                sourceStart = sourceStart + page.sourceStart,
                sourceEndExclusive = sourceStart + page.sourceEndExclusive,
            )
        }
        textRun.clear()
    }

    chapter.document.blocks.forEach { block ->
        if (block.requiresDedicatedPage()) {
            flushTextRun()
            provisional += HtmlProsePage(
                chapter = chapter.owner,
                index = 0,
                total = 1,
                text = block.renderedText,
                progression = chapter.document.document.progressionAt(
                    BookDocumentPosition(block.block.id, 0),
                ),
                sourceStart = block.block.logicalStart,
                sourceEndExclusive = block.block.logicalEndExclusive,
                structuredBlock = block,
            )
        } else {
            textRun += block
        }
    }
    flushTextRun()

    return provisional.mapIndexed { index, page ->
        page.copy(
            index = index,
            total = provisional.size,
            progression = if (index == provisional.lastIndex) {
                1f
            } else {
                page.sourceStart.toFloat()
                    .div(chapter.document.document.logicalExtent.coerceAtLeast(1))
                    .coerceIn(0f, 1f)
            },
        )
    }
}

private fun PreparedBookDocumentBlock.requiresDedicatedPage(): Boolean {
    val content = block.content
    if (content !is BookDocumentBlockContent.Text) return true
    if (content.preformatted) return true
    return block.style.backgroundArgb != null ||
        block.style.border != null ||
        block.style.paddingEm > 0f ||
        block.style.whiteSpace != BookDocumentWhiteSpace.NORMAL
}

private fun Spanned.withPaginationStyles(
    blockStyle: BookDocumentStyle,
    inlineStyles: List<BookDocumentInlineStyleRange>,
    resourceTypefaces: Map<String, Typeface>,
): Spanned = SpannableString(this).apply {
    if (blockStyle.hasInlinePaginationStyle()) {
        applyPaginationStyle(
            start = 0,
            endExclusive = length,
            style = BookDocumentInlineStyle(
                foregroundArgb = blockStyle.foregroundArgb,
                fontFamily = blockStyle.fontFamily,
                fontSizeScale = blockStyle.fontSizeScale.takeUnless { it == 1f },
                bold = blockStyle.bold,
            ),
            resourceTypefaces = resourceTypefaces,
        )
    }
    blockStyle.alignment?.let { blockAlignment ->
        setSpan(
            AlignmentSpan.Standard(blockAlignment.toLayoutAlignment()),
            0,
            length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    inlineStyles.forEach { range ->
        applyPaginationResourceTypeface(
            start = range.start,
            endExclusive = range.endExclusive,
            family = range.style.fontFamily as? BookDocumentFontFamily.Resource,
            resourceTypefaces = resourceTypefaces,
        )
    }
}

private fun Spannable.applyPaginationResourceTypeface(
    start: Int,
    endExclusive: Int,
    family: BookDocumentFontFamily.Resource?,
    resourceTypefaces: Map<String, Typeface>,
) {
    val typeface = family?.resourceId?.let(resourceTypefaces::get) ?: return
    val boundedStart = start.coerceIn(0, length)
    val boundedEnd = endExclusive.coerceIn(boundedStart, length)
    if (boundedEnd <= boundedStart) return
    setSpan(
        PaginationTypefaceSpan(typeface),
        boundedStart,
        boundedEnd,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
    )
}

private fun Spannable.applyPaginationStyle(
    start: Int,
    endExclusive: Int,
    style: BookDocumentInlineStyle?,
    resourceTypefaces: Map<String, Typeface>,
) {
    style ?: return
    val boundedStart = start.coerceIn(0, length)
    val boundedEnd = endExclusive.coerceIn(boundedStart, length)
    if (boundedEnd <= boundedStart) return
    val flags = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
    style.foregroundArgb?.let {
        setSpan(ForegroundColorSpan(it.toInt()), boundedStart, boundedEnd, flags)
    }
    style.backgroundArgb?.let {
        setSpan(BackgroundColorSpan(it.toInt()), boundedStart, boundedEnd, flags)
    }
    style.fontSizeScale?.let {
        setSpan(RelativeSizeSpan(it), boundedStart, boundedEnd, flags)
    }
    if (style.bold) {
        setSpan(StyleSpan(Typeface.BOLD), boundedStart, boundedEnd, flags)
    }
    if (style.italic) {
        setSpan(StyleSpan(Typeface.ITALIC), boundedStart, boundedEnd, flags)
    }
    if (style.underline) {
        setSpan(UnderlineSpan(), boundedStart, boundedEnd, flags)
    }
    if (style.strikethrough) {
        setSpan(StrikethroughSpan(), boundedStart, boundedEnd, flags)
    }
    if (style.subscript) {
        setSpan(SubscriptSpan(), boundedStart, boundedEnd, flags)
    }
    if (style.superscript) {
        setSpan(SuperscriptSpan(), boundedStart, boundedEnd, flags)
    }
    if (style.small && style.fontSizeScale == null) {
        setSpan(RelativeSizeSpan(SMALL_TEXT_SCALE), boundedStart, boundedEnd, flags)
    }
    if (style.code && style.fontFamily == null) {
        setSpan(TypefaceSpan("monospace"), boundedStart, boundedEnd, flags)
    }
    when (val family = style.fontFamily) {
        is BookDocumentFontFamily.Generic -> {
            val name = when (family.family) {
                BookDocumentFontFamily.GenericFamily.SERIF -> "serif"
                BookDocumentFontFamily.GenericFamily.SANS_SERIF -> "sans-serif"
                BookDocumentFontFamily.GenericFamily.MONOSPACE -> "monospace"
            }
            setSpan(TypefaceSpan(name), boundedStart, boundedEnd, flags)
        }
        is BookDocumentFontFamily.Resource -> resourceTypefaces[family.resourceId]?.let { typeface ->
            setSpan(PaginationTypefaceSpan(typeface), boundedStart, boundedEnd, flags)
        }
        null -> Unit
    }
}

private fun BookDocumentStyle.hasInlinePaginationStyle(): Boolean =
    foregroundArgb != null || fontFamily != null || fontSizeScale != 1f || bold

private const val SMALL_TEXT_SCALE = 0.8f

private fun BookDocumentAlignment.toLayoutAlignment(): Layout.Alignment = when (this) {
    BookDocumentAlignment.START -> Layout.Alignment.ALIGN_NORMAL
    BookDocumentAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
    BookDocumentAlignment.END -> Layout.Alignment.ALIGN_OPPOSITE
}

private class PaginationTypefaceSpan(
    private val typeface: Typeface,
) : MetricAffectingSpan() {
    override fun updateDrawState(textPaint: TextPaint) = textPaint.applyTypeface(typeface)
    override fun updateMeasureState(textPaint: TextPaint) = textPaint.applyTypeface(typeface)

    private fun Paint.applyTypeface(newTypeface: Typeface) {
        val previousStyle = typeface?.style ?: Typeface.NORMAL
        val missingStyle = previousStyle and newTypeface.style.inv()
        if (missingStyle and Typeface.BOLD != 0) isFakeBoldText = true
        if (missingStyle and Typeface.ITALIC != 0) textSkewX = -0.25f
        typeface = newTypeface
    }
}

internal fun pageIndexForAnchor(pages: List<HtmlProsePage>, anchorOffset: Int): Int? {
    return pages.indexOfFirst { page ->
        anchorOffset >= page.sourceStart &&
            (
                anchorOffset < page.sourceEndExclusive ||
                    (page.index == pages.lastIndex && anchorOffset == page.sourceEndExclusive)
                )
    }.takeIf { it >= 0 }
}
