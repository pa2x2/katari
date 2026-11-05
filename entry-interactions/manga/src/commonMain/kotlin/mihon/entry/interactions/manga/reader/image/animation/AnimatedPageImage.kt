package mihon.entry.interactions.manga.reader.image.animation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import mihon.core.image.animation.AnimatedImagePlayback
import mihon.core.image.asImageBitmap
import mihon.core.image.progressive.ProgressiveImageDecodeOptions
import mihon.entry.interactions.manga.reader.image.layout.PageImageLayout
import mihon.entry.interactions.manga.reader.image.layout.displayedSize
import mihon.entry.interactions.manga.reader.image.layout.inPiece
import mihon.entry.interactions.manga.reader.image.layout.pieces
import mihon.entry.interactions.manga.reader.image.source.PageImage
import kotlin.math.roundToInt

/**
 * Plays an animated page [image] in [layout], stretched over the composable's size, from its first frame on. Calls
 * [onFailed] when the image holds no frame the decoder can show.
 */
@Composable
internal fun AnimatedPageImage(
    image: PageImage.Animated,
    layout: PageImageLayout,
    displayProfile: ByteArray?,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var frame by remember(image) { mutableStateOf<ImageBitmap?>(null) }
    val currentOnFailed by rememberUpdatedState(onFailed)
    LaunchedEffect(image, displayProfile) {
        val playback = AnimatedImagePlayback(
            bytes = image.bytes,
            options = ProgressiveImageDecodeOptions(displayProfile = displayProfile),
        )
        val played = playback.play { frame = it.asImageBitmap() }
        if (!played) currentOnFailed()
    }
    val pieces = remember(image, layout) { layout.pieces(image.size) }
    val displayedSize = remember(image, layout) { layout.displayedSize(image.size) }
    Canvas(modifier) {
        val current = frame ?: return@Canvas
        if (displayedSize.width <= 0) return@Canvas
        // Frames may be decoded smaller than the image, to bound their memory
        val frameScale = current.width.toFloat() / image.size.width
        scale(size.width / displayedSize.width, pivot = Offset.Zero) {
            pieces.forEach { piece ->
                val srcOffset = IntOffset(
                    (piece.source.left * frameScale).roundToInt(),
                    (piece.source.top * frameScale).roundToInt(),
                )
                val srcSize = IntSize(
                    (piece.source.width * frameScale).roundToInt().coerceAtMost(current.width - srcOffset.x),
                    (piece.source.height * frameScale).roundToInt().coerceAtMost(current.height - srcOffset.y),
                )
                inPiece(piece) {
                    drawImage(
                        image = current,
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
