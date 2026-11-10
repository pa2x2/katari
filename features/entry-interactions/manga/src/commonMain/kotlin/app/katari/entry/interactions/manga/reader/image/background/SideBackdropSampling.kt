package app.katari.entry.interactions.manga.reader.image.background

import app.katari.core.image.argbPixels
import app.katari.core.image.pixelHeight
import app.katari.core.image.pixelWidth
import app.katari.core.image.release
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.PageImagePiece
import app.katari.entry.interactions.manga.reader.image.layout.QuarterTurn
import app.katari.entry.interactions.manga.reader.image.layout.pieces
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * What continues each piece of [image] shown in [layout] beside it ([chooseSideBackdrop]), in the order of its pieces,
 * or null for a piece that can't be decoded, as once the image is closed. The rules compare whole rows, so each piece is decoded whole, at
 * no fewer than [SIDE_BACKDROP_WIDTH] pixels across: enough to tell gaps from artwork, while a tall page decodes to a
 * few megabytes rather than tens.
 */
internal suspend fun PageImage.Still.chooseSideBackdrops(layout: PageImageLayout): List<List<BackdropSpan>?> =
    withContext(Dispatchers.Default) { layout.pieces(size).map { chooseSideBackdrop(it) } }

private suspend fun PageImage.Still.chooseSideBackdrop(piece: PageImagePiece): List<BackdropSpan>? {
    var sampleSize = 1
    while (piece.displayedSize.width / (sampleSize * 2) >= SIDE_BACKDROP_WIDTH) sampleSize *= 2
    val bitmap = decode(piece.source, sampleSize) ?: return null
    val width = bitmap.pixelWidth
    val height = bitmap.pixelHeight
    val pixels = try {
        bitmap.argbPixels()
    } finally {
        bitmap.release()
    }
    return chooseSideBackdrop(pixels.asPagePixels(width, height, piece.turn))
}

/** The [width] by [height] pixels, row by row, as the page shows them, [turn]ed. */
private fun IntArray.asPagePixels(width: Int, height: Int, turn: QuarterTurn): PagePixels = when (turn) {
    QuarterTurn.None -> PagePixels(width, height) { x, y -> this[y * width + x] }
    QuarterTurn.Clockwise -> PagePixels(height, width) { x, y -> this[(height - 1 - x) * width + y] }
    QuarterTurn.Anticlockwise -> PagePixels(height, width) { x, y -> this[x * width + (width - 1 - y)] }
}

private const val SIDE_BACKDROP_WIDTH = 200
