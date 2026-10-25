package mihon.entry.interactions.book.document.reader.box

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import mihon.book.api.document.BookDocumentBorder
import mihon.book.api.document.BookDocumentBorderStyle
import mihon.book.api.document.BookDocumentBoxStyle
import mihon.book.api.document.BookDocumentGradient
import mihon.entry.interactions.book.document.reader.theme.BookDocumentAuthoredStyling
import mihon.presentation.kit.content.DocumentBorder
import mihon.presentation.kit.content.DocumentBorderStyle
import mihon.presentation.kit.content.DocumentBorders
import mihon.presentation.kit.content.DocumentBoxDecoration
import mihon.presentation.kit.content.DocumentCorners
import mihon.presentation.kit.content.DocumentGradient
import mihon.presentation.kit.content.DocumentShadow
import mihon.presentation.kit.content.documentBox

/** Lays out and draws [slices], outermost first, around a block's content. */
internal fun Modifier.bookDocumentBoxes(
    slices: List<BookDocumentBoxSlice>,
    styling: BookDocumentAuthoredStyling,
    emDp: Float,
): Modifier = slices.fold(this) { modifier, slice -> modifier.boxSlice(slice, styling, emDp) }

/** The fitted background of the innermost slice that has one, which the block's text is drawn on. */
internal fun List<BookDocumentBoxSlice>.textBackdrop(styling: BookDocumentAuthoredStyling): Color =
    asReversed().firstNotNullOfOrNull { slice ->
        slice.style.backgroundArgb?.let(styling::fill)
            ?: slice.style.backgroundGradient?.stops?.first()?.colorArgb?.let(styling::fill)
    } ?: styling.palette.background

private fun Modifier.boxSlice(
    slice: BookDocumentBoxSlice,
    styling: BookDocumentAuthoredStyling,
    emDp: Float,
): Modifier {
    val margins = slice.margins(emDp)
    val frame = slice.frame(emDp)
    val decorated = if (slice.style.decorated) {
        Modifier.documentBox(slice.style.decoration(styling), !slice.opens, !slice.closes)
    } else {
        Modifier
    }
    return padding(margins.left, margins.top, margins.right, margins.bottom)
        .boxWidth(slice.style, emDp, fitContent = slice.style.fitContent && slice.opens && slice.closes)
        .then(decorated)
        .padding(frame.left, frame.top, frame.right, frame.bottom)
}

// A box narrower than its row sits at the start, or in the middle when its side margins are auto. Shrinking to the
// content only works for a box of one slice; the slices of a longer box would each get their own width.
private fun Modifier.boxWidth(style: BookDocumentBoxStyle, emDp: Float, fitContent: Boolean): Modifier {
    if (style.maxWidthEm == null && style.widthFraction == null && !fitContent) return this
    return layout { measurable, constraints ->
        if (!constraints.hasBoundedWidth) {
            val placeable = measurable.measure(constraints)
            return@layout layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
        }
        val available = constraints.maxWidth
        var width = style.widthPx(available, emDp.dp.toPx())
        if (fitContent) width = minOf(width, measurable.maxIntrinsicWidth(constraints.maxHeight))
        val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
        layout(available, placeable.height) {
            placeable.placeRelative(if (style.centered) (available - width) / 2 else 0, 0)
        }
    }
}

private fun BookDocumentBoxStyle.decoration(styling: BookDocumentAuthoredStyling): DocumentBoxDecoration {
    val lineColor = styling.palette.foreground
    return DocumentBoxDecoration(
        background = styling.fill(backgroundArgb),
        gradient = backgroundGradient?.let { gradient ->
            DocumentGradient(
                radial = gradient.kind == BookDocumentGradient.Kind.RADIAL,
                angleDegrees = gradient.angleDegrees,
                stops = gradient.stops.map { stop -> styling.fill(stop.colorArgb) to stop.position },
                repeating = gradient.repeating,
            )
        },
        borders = DocumentBorders(
            top = borders.top?.toKit(styling, lineColor),
            right = borders.right?.toKit(styling, lineColor),
            bottom = borders.bottom?.toKit(styling, lineColor),
            left = borders.left?.toKit(styling, lineColor),
        ),
        corners = with(cornerRadiiDp) { DocumentCorners(topLeft.dp, topRight.dp, bottomRight.dp, bottomLeft.dp) },
        shadows = shadows.map { shadow ->
            DocumentShadow(
                offsetX = shadow.offsetXDp.dp,
                offsetY = shadow.offsetYDp.dp,
                blur = shadow.blurDp.dp,
                spread = shadow.spreadDp.dp,
                color = shadow.colorArgb?.let(styling::fill) ?: lineColor,
                inset = shadow.inset,
            )
        },
    )
}

// A border without a colour takes the text colour, as CSS's currentcolor does.
private fun BookDocumentBorder.toKit(styling: BookDocumentAuthoredStyling, textColor: Color) = DocumentBorder(
    width = widthDp.dp,
    color = colorArgb?.let(styling::fill) ?: textColor,
    style = when (style) {
        BookDocumentBorderStyle.SOLID -> DocumentBorderStyle.Solid
        BookDocumentBorderStyle.DASHED -> DocumentBorderStyle.Dashed
        BookDocumentBorderStyle.DOTTED -> DocumentBorderStyle.Dotted
        BookDocumentBorderStyle.DOUBLE -> DocumentBorderStyle.Double
        BookDocumentBorderStyle.GROOVE -> DocumentBorderStyle.Groove
        BookDocumentBorderStyle.RIDGE -> DocumentBorderStyle.Ridge
        BookDocumentBorderStyle.INSET -> DocumentBorderStyle.Inset
        BookDocumentBorderStyle.OUTSET -> DocumentBorderStyle.Outset
    },
)
