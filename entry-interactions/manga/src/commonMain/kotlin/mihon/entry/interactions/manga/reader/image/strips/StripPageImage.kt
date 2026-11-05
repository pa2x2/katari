package mihon.entry.interactions.manga.reader.image.strips

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onSizeChanged
import mihon.entry.interactions.manga.reader.image.layout.PageImageLayout
import mihon.entry.interactions.manga.reader.image.source.PageImage
import mihon.entry.interactions.manga.reader.image.tiles.drawTiles
import mihon.entry.interactions.manga.reader.image.tiles.rememberTilePaint

/**
 * Draws a still page [image] in [layout], stretched over the composable's size, from strips decoded once for its
 * width. [onFailed] is told when the image can't be decoded. The caller sizes the composable to the displayed page's
 * aspect ratio.
 */
@Composable
internal fun StripPageImage(
    image: PageImage.Still,
    layout: PageImageLayout,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var width by remember { mutableIntStateOf(0) }
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
    val paint = rememberTilePaint()
    Canvas(modifier.onSizeChanged { width = it.width }) {
        val shown = strips ?: return@Canvas
        if (shown.displayedSize.width <= 0) return@Canvas
        scale(size.width / shown.displayedSize.width, pivot = Offset.Zero) {
            drawTiles(shown.pieces, shown.bands, paint)
        }
    }
}

private const val FRAMES_BEFORE_RELEASE = 2
