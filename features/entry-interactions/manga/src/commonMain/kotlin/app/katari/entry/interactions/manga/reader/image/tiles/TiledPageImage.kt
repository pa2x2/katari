package app.katari.entry.interactions.manga.reader.image.tiles

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import app.katari.entry.interactions.manga.reader.image.layout.PageImageLayout
import app.katari.entry.interactions.manga.reader.image.layout.rememberPageImagePaint
import app.katari.entry.interactions.manga.reader.image.source.PageImage
import kotlinx.coroutines.flow.collectLatest

/**
 * Draws a still page [image] in [layout], stretched over the composable's size, from tiles decoded at the resolution
 * [view] calls for. The caller sizes the composable to the displayed page's aspect ratio and reports, through [view],
 * which part of it is on screen and how far it is zoomed; a page off screen reports an empty visible area.
 */
@Composable
internal fun TiledPageImage(
    image: PageImage.Still,
    layout: PageImageLayout,
    view: () -> PageImageView,
    modifier: Modifier = Modifier,
) {
    val store = remember(image, layout) { PageTileStore(image, layout) }
    DisposableEffect(store) { onDispose { store.clear() } }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val currentView by rememberUpdatedState(view)
    LaunchedEffect(store) {
        snapshotFlow { currentView() to boxSize }.collectLatest { (view, size) -> store.load(this, view, size) }
    }
    val tilePaint = rememberPageImagePaint()
    Canvas(modifier.onSizeChanged { boxSize = it }) {
        val displayed = store.displayedSize
        if (displayed.width <= 0 || displayed.height <= 0) return@Canvas
        scale(size.width / displayed.width, size.height / displayed.height, Offset.Zero) {
            drawTiles(store.pieces, store.decoded, tilePaint)
        }
    }
}
