package app.katari.entry.interactions.manga.reader.image.strips

import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import app.katari.core.image.PlatformBitmap
import app.katari.core.image.copyRows
import app.katari.core.image.pixelHeight
import app.katari.core.image.pixelWidth
import app.katari.core.image.release
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.PageImagePiece
import app.katari.entry.interactions.manga.reader.image.layout.displayedSize
import app.katari.entry.interactions.manga.reader.image.layout.pieces
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import app.katari.entry.interactions.manga.reader.image.tiles.DecodedTile
import app.katari.entry.interactions.manga.reader.image.tiles.PageTile
import app.katari.entry.interactions.manga.reader.image.tiles.sampleSizeFor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/**
 * A still page decoded whole, as bands of rows each a bitmap of its own. The long strip shows pages whole and scrolls
 * through them fast, so it decodes each page once rather than in tiles: a region decoder reads every row above a
 * region again for most formats, which makes tiles of a tall page cost many times the page. Bands keep each upload to
 * the GPU small, so scrolling into a page does not stall on one huge texture.
 */
internal class PageStrips(
    val pieces: List<PageImagePiece>,
    val displayedSize: IntSize,
    /** Each band as a tile spanning the image's width, in the image's own pixels. */
    val bands: Map<PageTile, DecodedTile>,
) {
    /** Frees every band; the strips are not drawn afterwards. */
    fun release() = bands.values.forEach { it.pixels.release() }
}

/**
 * Decodes this page shown in [layout], [displayedWidth] screen pixels wide, at the coarsest resolution that keeps a
 * decoded pixel per screen pixel, coarser still when the page would otherwise take more than [MAX_PAGE_BYTES]. Returns
 * null when the image can't be decoded or was closed.
 */
internal suspend fun PageImage.Still.decodeStrips(layout: PageImageLayout, displayedWidth: Int): PageStrips? {
    val displayedSize = layout.displayedSize(size)
    if (displayedWidth <= 0 || displayedSize.width <= 0) return null
    var sampleSize = sampleSizeFor(displayedWidth.toFloat() / displayedSize.width)
    while (decodedBytes(size, sampleSize) > MAX_PAGE_BYTES) sampleSize *= 2

    return stripDecodes.withPermit {
        val page = decode(IntRect(0, 0, size.width, size.height), sampleSize) ?: return@withPermit null
        try {
            withContext(Dispatchers.Default) { PageStrips(layout.pieces(size), displayedSize, page.cutIntoBands(size)) }
        } finally {
            page.release()
        }
    }
}

// Bands of about BAND_BYTES each, so each takes about as long to upload; each band's region maps its decoded rows
// back onto the image's own
private fun PlatformBitmap.cutIntoBands(imageSize: IntSize): Map<PageTile, DecodedTile> {
    val bandRows = (BAND_BYTES / (pixelWidth.toLong() * BYTES_PER_PIXEL)).toInt().coerceIn(1, pixelHeight)
    val rowScale = imageSize.height.toFloat() / pixelHeight
    val bands = LinkedHashMap<PageTile, DecodedTile>()
    try {
        for (top in 0..<pixelHeight step bandRows) {
            val bottom = minOf(top + bandRows, pixelHeight)
            val region = IntRect(
                left = 0,
                top = (top * rowScale).toInt(),
                right = imageSize.width,
                bottom = if (bottom == pixelHeight) imageSize.height else (bottom * rowScale).toInt(),
            )
            bands[PageTile(region, sampleSize = 1)] = DecodedTile(copyRows(top, bottom))
        }
    } catch (e: Throwable) {
        bands.values.forEach { it.pixels.release() }
        throw e
    }
    return bands
}

private fun decodedBytes(size: IntSize, sampleSize: Int): Long =
    (size.width / sampleSize).toLong() * (size.height / sampleSize) * BYTES_PER_PIXEL

private const val BYTES_PER_PIXEL = 4

private const val BAND_BYTES = 4L * 1024 * 1024

private const val MAX_PAGE_BYTES = 128L * 1024 * 1024

// Each decode holds the whole page until its bands are copied, and the long strip shows several pages at once
private val stripDecodes = Semaphore(1)
