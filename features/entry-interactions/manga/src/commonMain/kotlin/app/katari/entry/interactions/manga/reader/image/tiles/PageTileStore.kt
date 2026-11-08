package app.katari.entry.interactions.manga.reader.image.tiles

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import app.katari.core.image.PlatformBitmap
import app.katari.core.image.asImageBitmap
import app.katari.core.image.release
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.PageImagePiece
import app.katari.entry.interactions.manga.reader.image.layout.displayedSize
import app.katari.entry.interactions.manga.reader.image.layout.pieces
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor

/** A decoded tile, kept as the platform bitmap to free it and as the bitmap Compose draws. */
internal class DecodedTile(val pixels: PlatformBitmap) {
    val image: ImageBitmap = pixels.asImageBitmap()
}

/**
 * Where a tiled page is seen: [visible] is the part of the page's own layout box on screen, in the box's pixels, and
 * [zoom] how far the viewer magnifies the box.
 */
internal data class PageImageView(val visible: IntRect, val zoom: Float)

/**
 * The tiles of one still page image that its view needs. A coarse layer covers the visible part and a screen beyond it
 * on each side at the resolution the page has unzoomed, so scrolling finds it decoded; a sharp layer covers only the
 * visible part at the zoomed resolution. Tiles the view no longer needs are dropped.
 */
internal class PageTileStore(private val image: PageImage.Still, layout: PageImageLayout) {
    val pieces: List<PageImagePiece> = layout.pieces(image.size)
    val displayedSize: IntSize = layout.displayedSize(image.size)

    /** Tiles decoded so far, read while drawing. */
    val decoded = mutableStateMapOf<PageTile, DecodedTile>()

    /**
     * Decodes the tiles [view] needs of a page laid out at [boxSize], coarse ones first, and drops the rest once the
     * screen no longer draws them. Returns when every needed tile is decoded.
     */
    suspend fun load(scope: CoroutineScope, view: PageImageView, boxSize: IntSize) {
        if (boxSize.width <= 0 || displayedSize.width <= 0) return
        val boxScale = boxSize.width.toFloat() / displayedSize.width
        val visible = view.visible.toImagePixels(boxScale)
        val margin = visible.height
        val nearby = IntRect(visible.left, visible.top - margin, visible.right, visible.bottom + margin)
        val coarseSample = sampleSizeFor(boxScale)
        val sharpSample = sampleSizeFor(boxScale * view.zoom)
        val wanted = buildList {
            pieces.forEach { addAll(tilesShowing(it, nearby, coarseSample)) }
            if (sharpSample < coarseSample) pieces.forEach { addAll(tilesShowing(it, visible, sharpSample)) }
        }
        val dropped = decoded.keys.filter { it !in wanted }.mapNotNull { decoded.remove(it) }
        if (dropped.isNotEmpty()) scope.launch { releaseAfterDrawn(dropped) }
        for (tile in wanted) {
            if (tile in decoded) continue
            val pixels = image.decode(tile.region, tile.sampleSize) ?: continue
            decoded[tile] = DecodedTile(pixels)
        }
    }

    // A frame being drawn, or one the platform renders behind the main thread, may still use the tile's pixels
    private suspend fun releaseAfterDrawn(tiles: List<DecodedTile>) {
        repeat(FRAMES_BEFORE_RELEASE) { withFrameNanos { } }
        tiles.forEach { it.pixels.release() }
    }

    /** Frees every tile; the store is not drawn afterwards. */
    fun clear() {
        decoded.values.forEach { it.pixels.release() }
        decoded.clear()
    }
}

private fun IntRect.toImagePixels(boxScale: Float) = IntRect(
    floor(left / boxScale).toInt(),
    floor(top / boxScale).toInt(),
    ceil(right / boxScale).toInt(),
    ceil(bottom / boxScale).toInt(),
)

private const val FRAMES_BEFORE_RELEASE = 2
