package mihon.entry.interactions.manga.reader.image.layout

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * Draws [bitmap], all of an image of [imageSize], in [layout], stretched over the composable's size. The bitmap may be
 * decoded smaller than the image, as animation frames and progressive previews are to bound their memory.
 */
@Composable
internal fun PageBitmap(
    bitmap: ImageBitmap,
    imageSize: IntSize,
    layout: PageImageLayout,
    modifier: Modifier = Modifier,
) {
    val pieces = remember(imageSize, layout) { layout.pieces(imageSize) }
    val displayedSize = remember(imageSize, layout) { layout.displayedSize(imageSize) }
    Canvas(modifier) {
        if (displayedSize.width <= 0 || imageSize.width <= 0) return@Canvas
        val bitmapScale = bitmap.width.toFloat() / imageSize.width
        scale(size.width / displayedSize.width, pivot = Offset.Zero) {
            pieces.forEach { piece ->
                val srcOffset = IntOffset(
                    (piece.source.left * bitmapScale).roundToInt(),
                    (piece.source.top * bitmapScale).roundToInt(),
                )
                val srcSize = IntSize(
                    (piece.source.width * bitmapScale).roundToInt().coerceAtMost(bitmap.width - srcOffset.x),
                    (piece.source.height * bitmapScale).roundToInt().coerceAtMost(bitmap.height - srcOffset.y),
                )
                inPiece(piece) {
                    drawImage(
                        image = bitmap,
                        srcOffset = srcOffset,
                        srcSize = srcSize,
                        dstSize = piece.source.size,
                        filterQuality = FilterQuality.Medium,
                    )
                }
            }
        }
    }
}
