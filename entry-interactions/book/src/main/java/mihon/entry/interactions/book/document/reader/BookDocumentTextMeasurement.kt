package mihon.entry.interactions.book.document.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Constraints
import mihon.book.api.document.BookDocumentBlock

/**
 * A text block laid out off-screen the way [BookDocumentSelectableText] draws it, for page breaks and float splits.
 * [trailingBreaks] are the paragraph's terminal line breaks, which the drawn text leaves out.
 */
internal class BookDocumentMeasuredText(
    val layout: TextLayoutResult,
    private val presentation: BookDocumentTextPresentation,
    val trailingBreaks: Int,
) {
    /** Offset in the block's text where [line] ends, without display-only characters. */
    fun sourceLineEnd(line: Int): Int {
        val end = layout.getLineEnd(line)
        return end - presentation.insertedOffsets.count { it < end }
    }

    /** Offset in the block's text where [line] starts. */
    fun sourceLineStart(line: Int): Int {
        val start = layout.getLineStart(line)
        return start - presentation.insertedOffsets.count { it < start }
    }
}

internal fun TextMeasurer.measureBookDocumentText(
    block: BookDocumentBlock,
    blockText: String,
    fonts: Map<String, FontFamily>,
    textScale: Float,
    width: Int,
): BookDocumentMeasuredText {
    val trailingBreaks = blockText.length - blockText.trimEnd('\n').length
    val presentation =
        bookDocumentTextPresentation(blockText.dropLast(trailingBreaks), block.inlineStyles, block.style.textTransform)
    val fontSize = bookDocumentFontSize(block, textScale, BOOK_DOCUMENT_BASE_TEXT_SIZE_SP)
    val annotated = presentation.toSelectableAnnotatedString(
        fonts,
        block.links,
        block.inlineStyles,
        "",
        fontSize.value,
        Color.Unspecified,
        {},
    )
    val layout = measure(
        text = annotated,
        style = bookDocumentTextStyle(block, fontSize, fonts),
        constraints = Constraints(maxWidth = width.coerceAtLeast(0)),
    )
    return BookDocumentMeasuredText(layout, presentation, trailingBreaks)
}
