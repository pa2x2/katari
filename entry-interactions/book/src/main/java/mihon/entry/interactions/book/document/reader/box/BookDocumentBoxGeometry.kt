package mihon.entry.interactions.book.document.reader.box

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBorder
import mihon.book.api.document.BookDocumentBoxStyle
import kotlin.math.roundToInt

/*
 * The room boxes take, shared by the composed rows and the off-screen page measurer so a page break lands where the
 * drawn page ends. [emDp] is the reader's em: the base text size times the user's text scale.
 */

internal data class BookDocumentBoxEdges(val left: Dp, val top: Dp, val right: Dp, val bottom: Dp)

/** Margins outside the slice's border; an open edge has none. */
internal fun BookDocumentBoxSlice.margins(emDp: Float): BookDocumentBoxEdges = with(style.marginEm) {
    BookDocumentBoxEdges(
        left = (left * emDp).dp,
        top = if (opens) (top * emDp).dp else 0.dp,
        right = (right * emDp).dp,
        bottom = if (closes) (bottom * emDp).dp else 0.dp,
    )
}

/** Border widths plus padding inside them; an open edge has neither. */
internal fun BookDocumentBoxSlice.frame(emDp: Float): BookDocumentBoxEdges = with(style) {
    BookDocumentBoxEdges(
        left = borders.left.width() + (paddingEm.left * emDp).dp,
        top = if (opens) borders.top.width() + (paddingEm.top * emDp).dp else 0.dp,
        right = borders.right.width() + (paddingEm.right * emDp).dp,
        bottom = if (closes) borders.bottom.width() + (paddingEm.bottom * emDp).dp else 0.dp,
    )
}

/** Width of the box, border included, within [availablePx]. */
internal fun BookDocumentBoxStyle.widthPx(availablePx: Int, emPx: Float): Int {
    var width = availablePx
    widthFraction?.let { fraction -> width = minOf(width, (availablePx * fraction).roundToInt()) }
    maxWidthEm?.let { maxWidth -> width = minOf(width, (maxWidth * emPx).roundToInt()) }
    return width.coerceAtLeast(0)
}

/** Width left for a block's content inside [slices], outermost first. */
internal fun Density.boxedContentWidthPx(slices: List<BookDocumentBoxSlice>, availablePx: Int, emDp: Float): Int =
    slices.fold(availablePx) { available, slice ->
        val margins = slice.margins(emDp)
        val frame = slice.frame(emDp)
        val box = slice.style.widthPx(available - margins.left.roundToPx() - margins.right.roundToPx(), emDp.dp.toPx())
        (box - frame.left.roundToPx() - frame.right.roundToPx()).coerceAtLeast(0)
    }

/** Height [slices] add above and below a block's content. */
internal fun Density.boxedVerticalPx(slices: List<BookDocumentBoxSlice>, emDp: Float): Int = slices.sumOf { slice ->
    val margins = slice.margins(emDp)
    val frame = slice.frame(emDp)
    margins.top.roundToPx() + margins.bottom.roundToPx() + frame.top.roundToPx() + frame.bottom.roundToPx()
}

private fun BookDocumentBorder?.width(): Dp = this?.widthDp?.dp ?: 0.dp
