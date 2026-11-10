package app.katari.entry.interactions.manga.reader.image.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntRect
import app.katari.core.image.PlatformBitmap
import app.katari.core.image.argbPixel
import app.katari.core.image.release
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.PageImagePiece
import app.katari.entry.interactions.manga.reader.image.layout.pieces
import app.katari.entry.interactions.manga.reader.image.layout.sourcePoint
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import kotlin.math.floor

/**
 * The background for each piece of [image] shown in [layout], in the order of its pieces, read at full resolution, as
 * the background rules count margins in pixels. Only the areas the rules read are decoded, so a tall page costs a few
 * narrow bands rather than its whole image. A piece that can't be decoded gets white.
 */
internal suspend fun PageImage.Still.chooseBackgrounds(
    layout: PageImageLayout,
    besidePage: Boolean,
): List<PageBackground> = layout.pieces(size).map { chooseBackground(it, besidePage) }

private suspend fun PageImage.Still.chooseBackground(piece: PageImagePiece, besidePage: Boolean): PageBackground {
    val size = piece.displayedSize
    if (size.width < MIN_PAGE_SIZE || size.height < MIN_PAGE_SIZE) return PageBackground.Solid(WHITE)
    val decoded = mutableListOf<DecodedArea>()
    try {
        for (area in PageBackgroundProbes(size.width, size.height).areas) {
            val source = piece.sourceShowing(area.translate(piece.offset)) ?: continue
            val bitmap = decode(source, sampleSize = 1) ?: return PageBackground.Solid(WHITE)
            decoded += DecodedArea(source, bitmap)
        }
        val pixels = PagePixels(size.width, size.height) { x, y ->
            // The centre of the displayed pixel, so a turned piece lands on the pixel it shows there
            val point = piece.sourcePoint(
                Offset(
                    x + piece.offset.x + HALF_PIXEL,
                    y + piece.offset.y + HALF_PIXEL,
                ),
            )
            val sourceX = floor(point.x).toInt()
            val sourceY = floor(point.y).toInt()
            val area = decoded.first { it.source.containsPixel(sourceX, sourceY) }
            area.bitmap.argbPixel(sourceX - area.source.left, sourceY - area.source.top)
        }
        return choosePageBackground(pixels, besidePage)
    } finally {
        decoded.forEach { it.bitmap.release() }
    }
}

/** [bitmap] decoded from the [source] area of the image. */
private class DecodedArea(val source: IntRect, val bitmap: PlatformBitmap)

private fun IntRect.containsPixel(x: Int, y: Int): Boolean = x in left..<right && y in top..<bottom

private const val HALF_PIXEL = 0.5f
