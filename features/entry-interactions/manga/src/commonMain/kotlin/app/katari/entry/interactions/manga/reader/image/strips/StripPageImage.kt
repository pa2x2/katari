package app.katari.entry.interactions.manga.reader.image.strips

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.rememberPageImagePaint
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import app.katari.entry.interactions.manga.reader.image.tiles.drawTiles

/**
 * Draws a still page [image] in [layout], stretched over the composable's size, from strips decoded once for [width],
 * the page's width on screen in pixels. [onFailed] is told when the image can't be decoded. The caller sizes the
 * composable to the displayed page's aspect ratio.
 *
 * The width comes from the caller rather than from measuring: a page composed ahead of the screen is not measured
 * again until it scrolls in, so a decode waiting for its size would start only once the page is already in view.
 */
@Composable
internal fun StripPageImage(
    image: PageImage.Still,
    layout: PageImageLayout,
    width: Int,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var strips by remember(image, layout) { mutableStateOf<PageStrips?>(null) }
    LaunchedEffect(image, layout, width) {
        if (width <= 0) return@LaunchedEffect
        val decoded = image.decodeStrips(layout, width)
        if (decoded == null) {
            // A closed image is being replaced by the page loading again, not unreadable
            if (!image.isClosed) onFailed()
            return@LaunchedEffect
        }
        val previous = strips
        strips = decoded
        // A frame being drawn, or one the platform renders behind the main thread, may still use the old bands
        if (previous != null) {
            try {
                repeat(FRAMES_BEFORE_RELEASE) { withFrameNanos { } }
            } finally {
                previous.release()
            }
        }
    }
    DisposableEffect(image, layout) { onDispose { strips?.release() } }
    val paint = rememberPageImagePaint()
    Canvas(modifier) {
        val shown = strips ?: return@Canvas
        if (shown.displayedSize.width <= 0 || shown.displayedSize.height <= 0) return@Canvas
        scale(size.width / shown.displayedSize.width, size.height / shown.displayedSize.height, Offset.Zero) {
            drawTiles(shown.pieces, shown.bands, paint)
        }
    }
}

private const val FRAMES_BEFORE_RELEASE = 2
