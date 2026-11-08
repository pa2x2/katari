package app.katari.entry.interactions.manga.reader.image.layout

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.IntOffset

/** Draws [block] in [piece]'s own coordinates, the image's pixels from the piece's top-left corner. */
internal inline fun DrawScope.inPiece(piece: PageImagePiece, crossinline block: DrawScope.() -> Unit) {
    val origin = when (piece.turn) {
        QuarterTurn.None -> piece.offset
        // Turning about the top-left corner swings the piece out of place, which these move it back from
        QuarterTurn.Clockwise -> piece.offset + IntOffset(piece.source.height, 0)
        QuarterTurn.Anticlockwise -> piece.offset + IntOffset(0, piece.source.width)
    }
    val degrees = when (piece.turn) {
        QuarterTurn.None -> 0f
        QuarterTurn.Clockwise -> 90f
        QuarterTurn.Anticlockwise -> -90f
    }
    translate(origin.x.toFloat(), origin.y.toFloat()) {
        rotate(degrees, pivot = Offset.Zero) { block() }
    }
}
