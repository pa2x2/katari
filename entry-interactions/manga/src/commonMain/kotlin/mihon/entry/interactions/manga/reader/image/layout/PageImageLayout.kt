package mihon.entry.interactions.manga.reader.image.layout

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import tachiyomi.core.common.util.system.SpreadSide

/**
 * How a viewer shows a page's image: as it is, or a double-page spread cut into its halves or turned to fit. The
 * viewers draw the pieces of the decoded image straight into place rather than encoding a cut or turned copy, so a
 * layout is described over the size of the image as decoded, after borders are cropped.
 */
internal sealed interface PageImageLayout {
    data object Whole : PageImageLayout

    /** One half of a spread on its own (dual-page split in the paged viewers). */
    data class Half(val side: SpreadSide) : PageImageLayout

    /** Both halves of a spread, [upper] above the other (dual-page split in the long strip). */
    data class Stacked(val upper: SpreadSide) : PageImageLayout

    /** The spread turned a quarter to fit the screen; a clockwise turn moves the image's left edge to the top. */
    data class Rotated(val clockwise: Boolean) : PageImageLayout
}

/**
 * The part [source] of the decoded image that the displayed page shows with its top-left corner at [offset], turned a
 * quarter when [turn] says so. [offset] is where the piece's displayed top-left corner lands, after any turn.
 */
internal data class PageImagePiece(val source: IntRect, val offset: IntOffset, val turn: QuarterTurn) {
    /** The piece's size on the displayed page. */
    val displayedSize: IntSize
        get() = if (turn == QuarterTurn.None) source.size else IntSize(source.height, source.width)

    /** The displayed page's area this piece covers. */
    val displayedBounds: IntRect
        get() = IntRect(offset, displayedSize)

    /** The part of [source] that shows within [displayed], an area of the displayed page, or null when none does. */
    fun sourceShowing(displayed: IntRect): IntRect? {
        val shown = displayed.intersect(displayedBounds)
        if (shown.width <= 0 || shown.height <= 0) return null
        // Relative to the piece's displayed top-left corner, then back into the image
        val left = shown.left - offset.x
        val top = shown.top - offset.y
        val right = shown.right - offset.x
        val bottom = shown.bottom - offset.y
        return when (turn) {
            QuarterTurn.None -> IntRect(left, top, right, bottom)
            QuarterTurn.Clockwise -> IntRect(top, source.height - right, bottom, source.height - left)
            QuarterTurn.Anticlockwise -> IntRect(source.width - bottom, left, source.width - top, right)
        }.translate(source.topLeft)
    }
}

internal enum class QuarterTurn { None, Clockwise, Anticlockwise }

/** The pieces that make up the displayed page for a decoded image of [imageSize]. */
internal fun PageImageLayout.pieces(imageSize: IntSize): List<PageImagePiece> {
    val whole = IntRect(IntOffset.Zero, imageSize)
    return when (this) {
        PageImageLayout.Whole -> listOf(PageImagePiece(whole, IntOffset.Zero, QuarterTurn.None))
        is PageImageLayout.Half -> listOf(PageImagePiece(imageSize.half(side), IntOffset.Zero, QuarterTurn.None))
        is PageImageLayout.Stacked -> listOf(
            PageImagePiece(imageSize.half(upper), IntOffset.Zero, QuarterTurn.None),
            PageImagePiece(imageSize.half(upper.opposite), IntOffset(0, imageSize.height), QuarterTurn.None),
        )
        is PageImageLayout.Rotated -> listOf(
            PageImagePiece(whole, IntOffset.Zero, if (clockwise) QuarterTurn.Clockwise else QuarterTurn.Anticlockwise),
        )
    }
}

/** The displayed page's size for a decoded image of [imageSize]. */
internal fun PageImageLayout.displayedSize(imageSize: IntSize): IntSize = when (this) {
    PageImageLayout.Whole -> imageSize
    is PageImageLayout.Half -> imageSize.half(side).size
    // The right half is the wider one when the width is odd
    is PageImageLayout.Stacked -> IntSize(imageSize.width - imageSize.width / 2, imageSize.height * 2)
    is PageImageLayout.Rotated -> IntSize(imageSize.height, imageSize.width)
}

/** Wider than tall: what the viewers take for a double-page spread. */
internal val IntSize.isSpread: Boolean
    get() = width > height

private fun IntSize.half(side: SpreadSide): IntRect = when (side) {
    SpreadSide.LEFT -> IntRect(0, 0, width / 2, height)
    SpreadSide.RIGHT -> IntRect(width / 2, 0, width, height)
}
