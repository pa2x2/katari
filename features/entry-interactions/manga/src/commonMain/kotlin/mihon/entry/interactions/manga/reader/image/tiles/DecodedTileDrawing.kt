package mihon.entry.interactions.manga.reader.image.tiles

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import mihon.entry.interactions.manga.reader.image.layout.PageImagePiece
import mihon.entry.interactions.manga.reader.image.layout.inPiece

/**
 * The paint tiles are drawn with. Antialiased edges would leave tiles that meet at a fractional pixel partly covered,
 * a line of the backdrop between them; without it, each pixel along the shared edge belongs to exactly one tile.
 */
@Composable
internal fun rememberTilePaint(): Paint = remember {
    Paint().apply {
        isAntiAlias = false
        filterQuality = FilterQuality.Medium
    }
}

/**
 * Draws decoded [tiles] of an image through its layout [pieces], in the displayed image's pixels. Coarse tiles go
 * first, so sharper ones cover them where both are decoded, and each piece shows only its own part of a tile.
 */
internal fun DrawScope.drawTiles(pieces: List<PageImagePiece>, tiles: Map<PageTile, DecodedTile>, paint: Paint) {
    val ordered = tiles.entries.sortedByDescending { it.key.sampleSize }
    pieces.forEach { piece ->
        inPiece(piece) {
            clipRect(right = piece.source.width.toFloat(), bottom = piece.source.height.toFloat()) {
                ordered.forEach { (tile, decoded) ->
                    if (piece.source.overlaps(tile.region)) {
                        drawIntoCanvas { canvas ->
                            canvas.drawImageRect(
                                image = decoded.image,
                                dstOffset = tile.region.topLeft - piece.source.topLeft,
                                dstSize = tile.region.size,
                                paint = paint,
                            )
                        }
                    }
                }
            }
        }
    }
}
