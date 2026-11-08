package app.katari.entry.interactions.manga.reader.image.background

import app.katari.core.image.PlatformBitmap
import app.katari.core.image.argbPixel
import app.katari.core.image.pixelHeight
import app.katari.core.image.pixelWidth
import app.katari.core.image.release
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.QuarterTurn
import app.katari.entry.interactions.manga.reader.image.layout.pieces
import app.katari.entry.interactions.manga.reader.image.source.PageImage

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
