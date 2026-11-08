package mihon.entry.interactions.manga.reader.image.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect

/**
 * Where the part of [rect], an area of the decoded image, that this piece shows lands on the displayed page, or null
 * when the piece shows none of it. The inverse of [PageImagePiece.sourceShowing].
 */
internal fun PageImagePiece.displayedShowing(rect: IntRect): IntRect? {
    val shown = rect.intersect(source)
    if (shown.width <= 0 || shown.height <= 0) return null
    // Relative to the piece's top-left corner in the image, then onto the displayed page
    val left = shown.left - source.left
    val top = shown.top - source.top
    val right = shown.right - source.left
    val bottom = shown.bottom - source.top
    return when (turn) {
        QuarterTurn.None -> IntRect(left, top, right, bottom)
        QuarterTurn.Clockwise -> IntRect(source.height - bottom, left, source.height - top, right)
        QuarterTurn.Anticlockwise -> IntRect(top, source.width - right, bottom, source.width - left)
    }.translate(offset)
}

/** The image point this piece shows at [displayed], a point of the displayed page, extended past the piece's edges. */
internal fun PageImagePiece.sourcePoint(displayed: Offset): Offset {
    val x = displayed.x - offset.x
    val y = displayed.y - offset.y
    val local = when (turn) {
        QuarterTurn.None -> Offset(x, y)
        QuarterTurn.Clockwise -> Offset(y, source.height - x)
        QuarterTurn.Anticlockwise -> Offset(source.width - y, x)
    }
    return local + source.topLeft.toOffset()
}

/** The piece that shows [displayed], a point of the displayed page, or the nearest one when it lies outside them all. */
internal fun List<PageImagePiece>.pieceAt(displayed: Offset): PageImagePiece? = minByOrNull { piece ->
    val bounds = piece.displayedBounds
    val dx = maxOf(bounds.left - displayed.x, 0f, displayed.x - bounds.right)
    val dy = maxOf(bounds.top - displayed.y, 0f, displayed.y - bounds.bottom)
    dx * dx + dy * dy
}

private fun IntOffset.toOffset() = Offset(x.toFloat(), y.toFloat())
