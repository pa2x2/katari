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
 * The [RowProfile] of each piece of [image] shown in [layout], in the order of its pieces, or null for a piece that
 * can't be decoded, as once the image is closed. A profile compares whole rows, so each piece is decoded whole, at no
 * fewer than [PROFILE_WIDTH] pixels across: enough to tell gaps from artwork, while a tall page decodes to a few
 * megabytes rather than tens.
 */
internal suspend fun PageImage.Still.rowProfiles(layout: PageImageLayout): List<RowProfile?> =
    withContext(Dispatchers.Default) { layout.pieces(size).map { rowProfile(it) } }

private suspend fun PageImage.Still.rowProfile(piece: PageImagePiece): RowProfile? {
    var sampleSize = 1
    while (piece.displayedSize.width / (sampleSize * 2) >= PROFILE_WIDTH) sampleSize *= 2
    val bitmap = decode(piece.source, sampleSize) ?: return null
    val width = bitmap.pixelWidth
    val height = bitmap.pixelHeight
    val pixels = try {
        bitmap.argbPixels()
    } finally {
        bitmap.release()
    }
    return pixels.asPagePixels(width, height, piece.turn).rowProfile()
}

/** The [width] by [height] pixels, row by row, as the page shows them, [turn]ed. */
private fun IntArray.asPagePixels(width: Int, height: Int, turn: QuarterTurn): PagePixels = when (turn) {
    QuarterTurn.None -> PagePixels(width, height) { x, y -> this[y * width + x] }
    QuarterTurn.Clockwise -> PagePixels(height, width) { x, y -> this[(height - 1 - x) * width + y] }
    QuarterTurn.Anticlockwise -> PagePixels(height, width) { x, y -> this[x * width + (width - 1 - y)] }
}

private const val PROFILE_WIDTH = 200
