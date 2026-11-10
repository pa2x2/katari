package app.katari.entry.interactions.manga.reader.image.tiles

import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import app.katari.entry.interactions.manga.reader.image.layout.PageImagePiece
import app.katari.entry.interactions.manga.reader.image.layout.inPiece

/**
 * Draws decoded [tiles] of an image through its layout [pieces], in the displayed image's pixels. Coarse tiles go
 * first, so sharper ones cover them where both are decoded, and each piece shows only its own part of a tile.
 */
internal fun DrawScope.drawTiles(pieces: List<PageImagePiece>, tiles: Map<PageTile, DecodedTile>, paint: Paint) {
    val ordered = tiles.entries.sortedByDescending { it.key.sampleSize }
    pieces.forEach { piece ->
        inPiece(piece) {
            clipToWholePixels(piece.source.width.toFloat(), piece.source.height.toFloat()) {
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
