package mihon.entry.interactions.manga.reader.image.background

import mihon.core.image.PlatformBitmap
import mihon.core.image.argbPixel
import mihon.core.image.pixelHeight
import mihon.core.image.pixelWidth
import mihon.core.image.release
import mihon.entry.interactions.manga.reader.image.layout.PageImageLayout
import mihon.entry.interactions.manga.reader.image.layout.QuarterTurn
import mihon.entry.interactions.manga.reader.image.layout.pieces
import mihon.entry.interactions.manga.reader.image.source.PageImage

/**
 * The background for [image] shown in a paged viewer's [layout], read from the page at full resolution, as the
 * background rules count margins in pixels. A page that can't be decoded gets white.
 */
internal suspend fun PageImage.Still.chooseBackground(layout: PageImageLayout, landscape: Boolean): PageBackground {
    // Paged layouts show one piece
    val piece = layout.pieces(size).first()
    val pixels = decode(piece.source, sampleSize = 1) ?: return PageBackground.Solid(WHITE)
    return try {
        choosePageBackground(pixels.asPagePixels(piece.turn), landscape)
    } finally {
        pixels.release()
    }
}

/** The pixels as the page shows them, [turn]ed. */
private fun PlatformBitmap.asPagePixels(turn: QuarterTurn): PagePixels {
    val width = pixelWidth
    val height = pixelHeight
    return when (turn) {
        QuarterTurn.None -> PagePixels(width, height) { x, y -> argbPixel(x, y) }
        QuarterTurn.Clockwise -> PagePixels(height, width) { x, y -> argbPixel(y, height - 1 - x) }
        QuarterTurn.Anticlockwise -> PagePixels(height, width) { x, y -> argbPixel(width - 1 - y, x) }
    }
}
