package mihon.entry.interactions.manga.reader.image.tiles

import androidx.compose.ui.unit.IntRect
import mihon.entry.interactions.manga.reader.image.layout.PageImagePiece

/** One tile of the decoded image: [region] of it, in its own pixels, decoded at 1/[sampleSize] of its resolution. */
internal data class PageTile(val region: IntRect, val sampleSize: Int)

/**
 * The coarsest sample size, a power of two, that still gives at least one decoded pixel per screen pixel when an image
 * pixel covers [screenPixelsPerImagePixel] screen pixels.
 */
internal fun sampleSizeFor(screenPixelsPerImagePixel: Float): Int {
    if (screenPixelsPerImagePixel <= 0f) return MAX_SAMPLE_SIZE
    var sampleSize = 1
    while (sampleSize < MAX_SAMPLE_SIZE && (sampleSize * 2) * screenPixelsPerImagePixel <= 1f) sampleSize *= 2
    return sampleSize
}

/**
 * The tiles at [sampleSize] that cover what [piece] shows within [displayed], an area of the displayed page. Tiles
 * sit on a grid fixed to the image, so the same tiles come back while the area moves, and each one decodes to at most
 * [TILE_PIXELS] pixels a side.
 */
internal fun tilesShowing(piece: PageImagePiece, displayed: IntRect, sampleSize: Int): List<PageTile> {
    val shown = piece.sourceShowing(displayed) ?: return emptyList()
    val side = TILE_PIXELS * sampleSize
    val source = piece.source
    val firstColumn = (shown.left - source.left) / side
    val lastColumn = (shown.right - 1 - source.left) / side
    val firstRow = (shown.top - source.top) / side
    val lastRow = (shown.bottom - 1 - source.top) / side
    return buildList {
        for (row in firstRow..lastRow) {
            for (column in firstColumn..lastColumn) {
                val left = source.left + column * side
                val top = source.top + row * side
                val region = IntRect(left, top, minOf(left + side, source.right), minOf(top + side, source.bottom))
                add(PageTile(region, sampleSize))
            }
        }
    }
}

/** A tile's side once decoded; small enough to decode quickly, large enough to keep the number of draws low. */
internal const val TILE_PIXELS = 1024

private const val MAX_SAMPLE_SIZE = 32
