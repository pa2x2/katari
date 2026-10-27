package mihon.entry.interactions.book.document.reader.box

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.takeOrElse
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

/**
 * Lays out and draws [slices], outermost first, around a block's content, on [surface], the surface around the
 * outermost one. A box can shrink to its content only when [shrinkable]: shrinking asks the content its intrinsic
 * width, which images, tables and rows can't answer.
 */
internal fun Modifier.bookDocumentBoxes(
    slices: List<BookDocumentBoxSlice>,
    styling: BookDocumentAuthoredStyling,
    emDp: Float,
    surface: Color,
    shrinkable: Boolean = false,
): Modifier = slices.fold(this to surface) { (modifier, around), slice ->
    modifier.boxSlice(slice, styling, emDp, shrinkable, around) to slice.style.surface(styling).takeOrElse { around }
}.first

/** The surface a block's text is drawn on inside [this], the slices around it, which sit on [outer]. */
internal fun List<BookDocumentBoxSlice>.textBackdrop(styling: BookDocumentAuthoredStyling, outer: Color): Color =
    fold(outer) { around, slice -> slice.style.surface(styling).takeOrElse { around } }

// A gradient's first stop stands for the whole gradient when deciding what text on it needs.
private fun BookDocumentBoxStyle.surface(styling: BookDocumentAuthoredStyling): Color =
    styling.surface(backgroundArgb).takeOrElse { styling.surface(backgroundGradient?.stops?.first()?.colorArgb) }

private fun Modifier.boxSlice(
    slice: BookDocumentBoxSlice,
    styling: BookDocumentAuthoredStyling,
    emDp: Float,
    shrinkable: Boolean,
    surface: Color,
): Modifier {
    val margins = slice.margins(emDp)
    val frame = slice.frame(emDp)
    val decorated = if (slice.style.decorated) {
        Modifier.documentBox(slice.style.decoration(styling, surface), !slice.opens, !slice.closes)
    } else {
        Modifier
    }
    return padding(margins.left, margins.top, margins.right, margins.bottom)
        .boxWidth(slice.style, emDp, fitContent = shrinkable && slice.style.fitContent && slice.opens && slice.closes)
        .then(decorated)
        .padding(frame.left, frame.top, frame.right, frame.bottom)
}

/** Draws [style]'s decoration over the whole node, on [surface], for a box whose layout its owner does itself. */
internal fun Modifier.bookDocumentBoxDecoration(
    style: BookDocumentBoxStyle,
    styling: BookDocumentAuthoredStyling,
    surface: Color,
): Modifier =
    if (style.decorated) {
        documentBox(
            style.decoration(styling, surface),
            continuesAbove = false,
            continuesBelow = false,
        )
    } else {
        this
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

/**
 * A box that paints its own background is a surface of the book, so its border and shadows, which edge that surface,
 * are drawn as written with it. A box without one draws them on the [surface] behind it.
 */
private fun BookDocumentBoxStyle.decoration(
    styling: BookDocumentAuthoredStyling,
    surface: Color,
): DocumentBoxDecoration {
    val ownSurface = surface(styling)
    val filled = ownSurface.isSpecified
    val lineColor = styling.inheritedText(styling.palette.foreground, ownSurface.takeOrElse { surface })
    val borderColor = { argb: Long -> if (filled) styling.surface(argb) else styling.line(argb, surface) }
    val boxShadowColor = { argb: Long -> if (filled) styling.surface(argb) else styling.shadowColor(argb, surface) }
    return DocumentBoxDecoration(
        background = styling.surface(backgroundArgb),
        gradient = backgroundGradient?.let { gradient ->
            DocumentGradient(
                radial = gradient.kind == BookDocumentGradient.Kind.RADIAL,
                angleDegrees = gradient.angleDegrees,
                stops = gradient.stops.map { stop -> styling.surface(stop.colorArgb) to stop.position },
                repeating = gradient.repeating,
            )
        },
        borders = DocumentBorders(
            top = borders.top?.toKit(borderColor, lineColor),
            right = borders.right?.toKit(borderColor, lineColor),
            bottom = borders.bottom?.toKit(borderColor, lineColor),
            left = borders.left?.toKit(borderColor, lineColor),
        ),
        corners = with(cornerRadiiDp) { DocumentCorners(topLeft.dp, topRight.dp, bottomRight.dp, bottomLeft.dp) },
        shadows = shadows.map { shadow ->
            DocumentShadow(
                offsetX = shadow.offsetXDp.dp,
                offsetY = shadow.offsetYDp.dp,
                blur = shadow.blurDp.dp,
                spread = shadow.spreadDp.dp,
                color = shadow.colorArgb?.let(boxShadowColor) ?: lineColor,
                inset = shadow.inset,
            )
        },
    )
}

// A border without a colour takes the text colour, as CSS's currentcolor does.
private fun BookDocumentBorder.toKit(color: (Long) -> Color, textColor: Color) = DocumentBorder(
    width = widthDp.dp,
    color = colorArgb?.let(color) ?: textColor,
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
