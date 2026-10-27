package mihon.entry.interactions.book.document.reader.floats

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentFloat
import mihon.entry.interactions.book.document.reader.box.BookDocumentBoxSlice
import mihon.entry.interactions.book.document.reader.box.boxedContentWidthPx

/*
 * The slot a floated block takes: its width at its side of the row. Text wraps beside it in the scrolling reader;
 * elsewhere the float stacks above the text but keeps its slot, and the page measurer sizes it the same way.
 */

/** Width of a float within [availablePx]. */
internal fun BookDocumentFloat.widthPx(availablePx: Int, emPx: Float): Int =
    (widthEm?.times(emPx) ?: (availablePx * (widthFraction ?: DEFAULT_FRACTION)))
        .toInt()
        .coerceIn(1, (availablePx * MAX_FRACTION).toInt().coerceAtLeast(1))

/** Splits a floated block's slices, outermost first, into the boxes around its slot and its [ownBoxes] innermost. */
internal fun List<BookDocumentBoxSlice>.splitAtFloat(
    ownBoxes: Int,
): Pair<List<BookDocumentBoxSlice>, List<BookDocumentBoxSlice>> {
    val split = (size - ownBoxes).coerceAtLeast(0)
    return subList(0, split) to subList(split, size)
}

/** Places the content in the float's slot when the float stacks instead of wrapping. */
internal fun Modifier.bookDocumentFloatSlot(float: BookDocumentFloat, emDp: Float): Modifier =
    layout { measurable, constraints ->
        val available = constraints.maxWidth
        val width = float.widthPx(available, emDp.dp.toPx())
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(available, placeable.height) {
            placeable.place(if (float.side == BookDocumentFloat.Side.LEFT) 0 else available - width, 0)
        }
    }

/** Width left for a stacked float's content inside [slices], as [bookDocumentFloatSlot] lays it out. */
internal fun Density.floatContentWidthPx(
    float: BookDocumentFloat,
    slices: List<BookDocumentBoxSlice>,
    ownBoxes: Int,
    availablePx: Int,
    emDp: Float,
): Int {
    val (around, own) = slices.splitAtFloat(ownBoxes)
    val slot = float.widthPx(boxedContentWidthPx(around, availablePx, emDp), emDp.dp.toPx())
    return boxedContentWidthPx(own, slot, emDp)
}

// CSS shrinks a float to its content; without a width a book's float gets a share that leaves room for text.
private const val DEFAULT_FRACTION = 0.4f
private const val MAX_FRACTION = 0.6f
