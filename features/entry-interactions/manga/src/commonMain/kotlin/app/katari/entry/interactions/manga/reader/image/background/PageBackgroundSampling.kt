package app.katari.entry.interactions.manga.reader.image.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntRect
import app.katari.core.image.PlatformBitmap
import app.katari.core.image.argbPixel
import app.katari.core.image.release
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.pieces
import app.katari.entry.interactions.manga.reader.image.layout.sourcePoint
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import kotlin.math.floor

/**
 * The background for [image] shown in a paged viewer's [layout], read at full resolution, as the background rules
 * count margins in pixels. Only the areas the rules read are decoded rather than the whole image. A page that can't be
 * decoded gets white.
 */
internal suspend fun PageImage.Still.chooseBackground(layout: PageImageLayout, besidePage: Boolean): PageBackground {
    // Paged layouts show one piece
    val piece = layout.pieces(size).first()
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
